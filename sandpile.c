
// sandpile.c
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <time.h>

typedef struct {
    int n;              // logical n
    int stride;         // n + 2
    int size;           // stride * stride
    uint32_t *z;             // heights (flat with ghost border)
    uint8_t *interior;  // 1 for interior cells, 0 for ghost
    uint8_t *inQ;       // queue membership flags
    int *q;             // circular queue storage
    int head, tail;     // queue pointers
    // neighbor offsets
    int offN, offS, offE, offW;
    // RNG state (xorshift32 for demo; swap in a better PRNG for production)
    uint32_t rng;
    uint64_t outflow;
} Sandpile;

static inline uint32_t xs32(uint32_t *s) {
    uint32_t x = *s;
    x ^= x << 13; x ^= x >> 17; x ^= x << 5;
    *s = x;
    return x;
}

static inline int randint(Sandpile *sp, int bound) {
    // returns [0..bound-1], bound <= 2^31
    return (int)(xs32(&sp->rng) % (uint32_t)bound);
}

Sandpile *sp_create(int n, uint32_t seed) {
    Sandpile *sp = (Sandpile*)malloc(sizeof(Sandpile));

    if (!sp) { fprintf(stderr, "alloc sp failed\n"); exit(1); }

    sp->n = n;
    sp->stride = n + 2;
    sp->size = sp->stride * sp->stride;

    sp->z = (uint32_t*)calloc((size_t)sp->size, sizeof(uint32_t));
    sp->interior = (uint8_t*)calloc((size_t)sp->size, sizeof(uint8_t));
    sp->inQ = (uint8_t*)calloc((size_t)sp->size, sizeof(uint8_t));
    sp->q = (int*)malloc((size_t)sp->size * sizeof(int));

    if (!sp->z || !sp->interior || !sp->inQ || !sp->q) {
        fprintf(stderr, "alloc arrays failed\n"); exit(1);
    }

    sp->head = sp->tail = 0;
    sp->offN = -sp->stride; sp->offS = +sp->stride; sp->offE = +1; sp->offW = -1;
    sp->rng = seed ? seed : 0xDEADBEEFu;
    sp->outflow = 0;

    // interior mask: true on [1..n] x [1..n]
    for (int i = 1; i <= n; i++) {
        int base = i * sp->stride;
        for (int j = 1; j <= n; j++) {
            sp->interior[base + j] = 1;
        }
    }
    return sp;
}

void sp_free(Sandpile *sp) {
    if (!sp) return;
    free(sp->z);
    free(sp->interior);
    free(sp->inQ);
    free(sp->q);
    free(sp);
}

static inline void sp_reset(Sandpile *sp) {
    memset(sp->z, 0, (size_t)sp->size * sizeof(uint32_t));
    memset(sp->inQ, 0, (size_t)sp->size * sizeof(uint8_t));
    sp->head = sp->tail = 0;
    sp->outflow = 0;
}

static inline void sp_cold_start(Sandpile *sp) { sp_reset(sp); }

static inline void sp_hot_start(Sandpile *sp) {
    sp_reset(sp);
    for (int i = 1; i <= sp->n; i++) {
        int base = i * sp->stride;
        for (int j = 1; j <= sp->n; j++) {
            sp->z[base + j] = (uint32_t)randint(sp, 4);
        }
    }
}

static inline void sp_unstable_start(Sandpile *sp) {
    sp_reset(sp);
    for (int i = 1; i <= sp->n; i++) {
        int base = i * sp->stride;
        for (int j = 1; j <= sp->n; j++) {
            sp->z[base + j] = 3u;
        }
    }
}

static inline void q_clear(Sandpile *sp) { sp->head = sp->tail = 0; }
static inline int q_empty(Sandpile *sp) { return sp->head == sp->tail; }

static inline void q_add(Sandpile *sp, int v) {
    int next = sp->tail + 1;
    if (next == sp->size) next = 0;
    if (next == sp->head) {
        fprintf(stderr, "queue overflow (size=%d)\n", sp->size);
        exit(1);
    }
    sp->q[sp->tail++] = v;
    sp->tail = next;
}
static inline int q_poll(Sandpile *sp) {
    int v = sp->q[sp->head++];
    if (sp->head == sp->size) sp->head = 0;
    return v;
}

// Returns avalanche size (# of topples) for one grain drop
int sp_drop_and_relax(Sandpile *sp) {
    // Choose a random interior cell: indices 1..n
    int i = 1 + randint(sp, sp->n);
    int j = 1 + randint(sp, sp->n);
    int c = i * sp->stride + j;

    uint32_t h = ++sp->z[c];
    if (h < 4u) return 0;

    q_clear(sp);
    q_add(sp, c);
    sp->inQ[c] = 1;

    int topples = 0;

    while (!q_empty(sp)) {
        int u = q_poll(sp);
        sp->inQ[u] = 0;

        uint32_t hu = sp->z[u];
        if (hu < 4u) continue;

        // Batch topples
        uint32_t times = hu >> 2;           // /4
        sp->z[u] = hu - (times << 2);  // %4
        topples += (int)times;

        // Neighbors
        int nN = u + sp->offN;
        int nS = u + sp->offS;
        int nE = u + sp->offE;
        int nW = u + sp->offW;

        // N
        if (sp->interior[nN]) {
            uint32_t t = (sp->z[nN] += times);
            if (t >= 4u && !sp->inQ[nN]) { q_add(sp, nN); sp->inQ[nN] = 1; }
        } else {
            sp->outflow += times;
        }
        // S
        if (sp->interior[nS]) {
            uint32_t t = (sp->z[nS] += times);
            if (t >= 4u && !sp->inQ[nS]) { q_add(sp, nS); sp->inQ[nS] = 1; }
        } else {
            sp->outflow += times;
        }
        // E
        if (sp->interior[nE]) {
            uint32_t t = (sp->z[nE] += times);
            if (t >= 4u && !sp->inQ[nE]) { q_add(sp, nE); sp->inQ[nE] = 1; }
        } else {
            sp->outflow += times;
        }
        // W
        if (sp->interior[nW]) {
            uint32_t t = (sp->z[nW] += times);
            if (t >= 4u && !sp->inQ[nW]) { q_add(sp, nW); sp->inQ[nW] = 1; }
        } else {
            sp->outflow += times;
        }
    }
    return topples;
}


// Example driver (time a bunch of drops)
int main(void) {
    int n = 256;            // adjust as needed
    int warmup = 700000;    // warmup drops
    int drops = 250000;    // measured drops

    Sandpile *sp = sp_create(n, 42u);
    sp_hot_start(sp);

    // Warmup
    for (int i = 0; i < warmup; i++) sp_drop_and_relax(sp);

    // Time the main run
    clock_t t0 = clock();
    long sumTopples = 0;
    for (int i = 0; i < drops; i++) {
        sumTopples += sp_drop_and_relax(sp);
    }
    clock_t t1 = clock();

    double secs = (double)(t1 - t0) / CLOCKS_PER_SEC;
    printf("n=%d, drops=%d, time=%.3f s, avg topples/drop=%.3f\n",
           n, drops, secs, (double)sumTopples / drops);

    sp_free(sp);
    return 0;
}
