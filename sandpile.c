
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
    int *z;             // heights (flat with ghost border)
    uint8_t *interior;  // 1 for interior cells, 0 for ghost
    uint8_t *inQ;       // queue membership flags
    int *q;             // circular queue storage
    int head, tail;     // queue pointers
    // neighbor offsets
    int offN, offS, offE, offW;
    // RNG state (xorshift32 for demo; swap in a better PRNG for production)
    uint32_t rng;
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
    sp->n = n;
    sp->stride = n + 2;
    sp->size = sp->stride * sp->stride;
    sp->z = (int*)calloc((size_t)sp->size, sizeof(int));
    sp->interior = (uint8_t*)calloc((size_t)sp->size, sizeof(uint8_t));
    sp->inQ = (uint8_t*)calloc((size_t)sp->size, sizeof(uint8_t));
    sp->q = (int*)malloc((size_t)sp->size * sizeof(int));
    sp->head = sp->tail = 0;
    sp->offN = -sp->stride; sp->offS = +sp->stride; sp->offE = +1; sp->offW = -1;
    sp->rng = seed ? seed : 0xDEADBEEF;

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
    memset(sp->z, 0, sp->size * sizeof(int));
    memset(sp->inQ, 0, sp->size * sizeof(uint8_t));
    sp->head = sp->tail = 0;
}

static inline void sp_cold_start(Sandpile *sp) { sp_reset(sp); }

static inline void sp_hot_start(Sandpile *sp) {
    sp_reset(sp);
    for (int i = 1; i <= sp->n; i++) {
        int base = i * sp->stride;
        for (int j = 1; j <= sp->n; j++) {
            sp->z[base + j] = randint(sp, 4);
        }
    }
}

static inline void sp_unstable_start(Sandpile *sp) {
    sp_reset(sp);
    for (int i = 1; i <= sp->n; i++) {
        int base = i * sp->stride;
        for (int j = 1; j <= sp->n; j++) {
            sp->z[base + j] = 3;
        }
    }
}

static inline void q_clear(Sandpile *sp) { sp->head = sp->tail = 0; }
static inline void q_add(Sandpile *sp, int v) {
    sp->q[sp->tail++] = v;
    if (sp->tail == sp->size) sp->tail = 0;
}
static inline int q_poll(Sandpile *sp) {
    int v = sp->q[sp->head++];
    if (sp->head == sp->size) sp->head = 0;
    return v;
}
static inline int q_empty(Sandpile *sp) { return sp->head == sp->tail; }

// Returns avalanche size (# of topples) for one grain drop
int sp_drop_and_relax(Sandpile *sp) {
    // Choose a random interior cell: indices 1..n
    int i = 1 + randint(sp, sp->n);
    int j = 1 + randint(sp, sp->n);
    int c = i * sp->stride + j;

    int h = ++sp->z[c];
    if (h < 4) return 0;

    q_clear(sp);
    q_add(sp, c);
    sp->inQ[c] = 1;

    int topples = 0;

    while (!q_empty(sp)) {
        int u = q_poll(sp);
        sp->inQ[u] = 0;

        int hu = sp->z[u];
        if (hu < 4) continue;

        // Batch topples
        int times = hu >> 2;           // /4
        sp->z[u] = hu - (times << 2);  // %4
        topples += times;

        // Neighbors
        int nN = u + sp->offN;
        int nS = u + sp->offS;
        int nE = u + sp->offE;
        int nW = u + sp->offW;

        // North
        int t = (sp->z[nN] += times);
        if (sp->interior[nN] && t >= 4 && !sp->inQ[nN]) { q_add(sp, nN); sp->inQ[nN] = 1; }

        // South
        t = (sp->z[nS] += times);
        if (sp->interior[nS] && t >= 4 && !sp->inQ[nS]) { q_add(sp, nS); sp->inQ[nS] = 1; }

        // East
        t = (sp->z[nE] += times);
        if (sp->interior[nE] && t >= 4 && !sp->inQ[nE]) { q_add(sp, nE); sp->inQ[nE] = 1; }

        // West
        t = (sp->z[nW] += times);
        if (sp->interior[nW] && t >= 4 && !sp->inQ[nW]) { q_add(sp, nW); sp->inQ[nW] = 1; }
    }
    return topples;
}

// Example driver (time a bunch of drops)
int main(void) {
    int n = 512;            // adjust as needed
    int warmup = 10000;    // warmup drops
    int drops = 1000000;    // measured drops

    Sandpile *sp = sp_create(n, 42u);
    sp_unstable_start(sp);

    // Warmup
    for (int i = 0; i < warmup; i++) sp_drop_and_relax(sp);

    // Time the main run
    clock_t t0 = clock();
    long long sumTopples = 0;
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
