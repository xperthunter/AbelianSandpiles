import java.util.*;
import java.util.stream.Collectors;

/**
    * Abelian Sandpile Simulation (open boundary)
    * Grid: N x N
    * Drop a grain at a random cell
    * Topple rule: while cell >= 4, send 1 grain to each neighbor
    * Avalanche size = total number of toppling events after a drop
    *
    * Compile: javac SandpileSimulation.java
    * Run:     java SandpileSimulation
*/

public class SandpileSimulation {

    public static class Sandpile {
        private final int n;                    // grid size (n x n)
        private final int [][] z;               // heights
        private final Random rng;
        private final int[] dr = {-1, 1, 0, 0}; // N, S, E, W
        private final int[] dc = {0, 0, 1, -1};

        public Sandpile(int n, long seed) {
            if (n <= 0) throw new IllegalArgumentException("n must be positive");
            this.n = n;
            this.z = new int[n][n];
            this.rng = new Random(seed);
        }

        public int size() {return n; }

        public int[][] snapshot() {
            int[][] copy = new int[n][n];
            for (int i = 0; i < n; i++) System.arraycopy(z[i], 0, copy[i], 0, n);
            return copy;
        }

        /**
         * drop one grain at a random cell and fully relax
         * @return avalance size = total number of toppling events
        */

        public int dropGrainAndRelax() {
            int r = rng.nextInt(n);
            int c = rng.nextInt(n);
            z[r][c]++;

            // if no instability, no avalance
            if (z[r][c] < 4) return 0;

            // efficient relaxation with a queue
            // process only unstable site
            // avoid recursion to prevent stack overflows

            ArrayDeque<int[]> q = new ArrayDeque<>();
            boolean[][] inQueue = new boolean[n][n];
            q.add(new int[]{r,c});
            inQueue[r][c] = true;

            int avalanceTopplings = 0;

            while(!q.isEmpty()) {
                int[] cell = q.poll();
                int i = cell[0], j = cell[1];
                inQueue[i][j] = false;

                // if stable now, continue
                if (z[i][j] < 4) continue;

                // topple as many times as necessary in one shot
                int times = z[i][j] / 4;
                z[i][j] -= times * 4;
                avalanceTopplings += times;

                // Distribute to neighbors
                for (int k = 0; k < 4; k++) {
                    int ni = i + dr[k];
                    int nj = j + dc[k];
                    if (ni >= 0 && ni < n && nj >= 0 && nj < n) {
                        z[ni][nj] += times;
                        if (z[ni][nj] >= 4 && !inQueue[ni][nj]) {
                            q.add(new int[]{ni, nj});
                            inQueue[ni][nj] = true;
                        }
                    }
                    // else: grain escapess the system (open boundary)
                }

                // if (i,j) became unstable again due to future neighbor additions,
                // it will get re-queued when that happens
            }

            return avalanceTopplings;
        }

        public List<Integer> runDrops(int drops) {
            ArrayList<Integer> sizes = new ArrayList<>(drops);
            for (int t = 0; t < drops; t++) {
                sizes.add(dropGrainAndRelax());
            }
            return sizes;
        }

        // pretty print a grid
        public void print(int maxSizeToPrint) {
            if (n > maxSizeToPrint) {
                System.out.println("Grid too large to print; n=" + n);
                return;
            }
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    System.out.print(z[i][j]);
                    if (j < n - 1) {
                        System.out.print(" ");
                    }
                }
                System.out.println();
            }
        }
    }

    // simple statistics helper
    public static class Stats {
        public final long count;
        public final long sum;
        public final int min;
        public final int max;
        public final double mean;

        public Stats(List<Integer> data) {
            if (data.isEmpty()) {
                count = 0; sum = 0; min = 0; max = 0; mean = Double.NaN;
                return;
            }
            long s = 0;
            int mn = Integer.MAX_VALUE, mx = Integer.MIN_VALUE;
            for (int v: data) {
                s += v;
                if (v < mn) mn = v;
                if (v > mx) mx = v;
            }

            this.count = data.size();
            this.sum = s;
            this.min = mn;
            this.max = mx;
            this.mean = s / (double) count;
        }
    }

    public static void main(String[] args) {
        final int N = 32;
        final int DROPS = 5000000;
        final long SEED = 42L;

        Sandpile sp = new Sandpile(N, SEED);

        System.out.println("Running sandpile with N=" + N + ", drops=" + DROPS + "...");
        long t0 = System.nanoTime();
        List<Integer> avalancheSizes = sp.runDrops(DROPS);
        long t1 = System.nanoTime();

        Stats st = new Stats(avalancheSizes);
        System.out.printf(Locale.US, "Done in %.3f s%n", (t1-t0)/1e9);
        System.out.printf("Avalance sizes: count=%d, min=%d, max=%d, mean=%.4f%n",
            st.count, st.min, st.max, st.mean);

        // fraction of zero avalanche  drops (no toppling)
        long zeros = avalancheSizes.stream().filter(v -> v == 0).count();
        System.out.printf("Zero-size avalanches: %d (%.2f%%)%n",
            zeros, 100.0 * zeros / Math.max(1, st.count));

        sp.print(32);

        System.out.println("Hello, sand pile!");
    }
}










































/**
random number generator variations
Variation: For better statistical quality and concurrency, many modern Java programs use java.util.random.RandomGenerator implementations (e.g., SplittableRandom, Xoroshiro/Xoshiro via 3rd‑party libs) or ThreadLocalRandom when you don’t need reproducible seeding. For your learning project, Random(seed) is perfect.
*/
