import java.util.*;
import java.util.stream.Collectors;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;


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

    /**
        Class to simulate a sanpile.
            * initializtion : how to initialize the pile
                - `cold` (default) : every site initialized at 0
                - `hot`            : every site initialized randomly [0,1,2,3,4]
                - `unstable`       : every site initialized to 3
            * rng  : Random number generator (RandomGenerator type)
            * seed : seed for reproducibility
    */

    private final String start;
    private final String rngMethod;
    private final long seed;
    public final RandomGenerator rng; // RandomGenerator can handle any type of generator
    								  // generator gets passed in by name

    // default constructor
    public SandpileSimulation() {
        this("cold", "Random", 42L);
    }

    // constructor with args
    public SandpileSimulation(String start, String rngMethod, long seed) {
        if (!start.equals("hot") && !start.equals("cold") && !start.equals("unstable")) {
            throw new IllegalArgumentException("start must be [hot], [cold], or [unstable]");
        }

        this.start = start;
        this.rngMethod = rngMethod;
        this.seed  = seed;

        this.rng = RandomGeneratorFactory.of(rngMethod).create(seed);
    }

    // sandpile class -- contains methods that alter the sandpile state
    public static class Sandpile {
        public final int n;                     // grid size (n x n)
        private final int [][] z;               // heights
        private final int[] dr = {-1, 1, 0, 0}; // N, S, E, W
        private final int[] dc = {0, 0, 1, -1};
        RandomGenerator rng;

        // constructor
        public Sandpile(int n, RandomGenerator rng) {
            if (n <= 0) throw new IllegalArgumentException("n must be positive");
            this.n = n;
            this.z = new int[n][n];
            this.rng = rng;
        }
        
        public int size() {return n; }

        public int[][] snapshot() {
            int[][] copy = new int[n][n];
            for (int i = 0; i < n; i++) System.arraycopy(z[i], 0, copy[i], 0, n);
            return copy;
        }
        
        public void reset() {
            for (int i = 0; i < n; i++) {
                java.util.Arrays.fill(z[i], 0);
            }
        }

        // grid set to all zeros
        public void coldStart() {
            reset();
        }
		
		// cells set to 0,1,2,3 randomly (uniformly)
        public void hotStart() {
            int val;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    val = rng.nextInt(4);
                    z[i][j] = val;
                }
            }
        }
        
        // every cell set to 3
        public void unstableStart() {
            for (int i = 0; i < n; i++) {
                java.util.Arrays.fill(z[i], 3);
            }
        }
        
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

        // fix this -- printing is not quite right
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
    
    // make a sandpile in the simulation
    private Sandpile sandpile;
    public void makeSandpile (int n) {
        this.sandpile = new Sandpile(n, this.rng);
    }

    // initialize the sandpile
    public void initialize() {
        if (start.equals("hot"))           { sandpile.hotStart(); }
        else if (start.equals("cold"))     { sandpile.coldStart(); }
        else if (start.equals("unstable")) { sandpile.unstableStart(); }
        else {
            throw new IllegalArgumentException("start must be [hot], [cold], or [unstable]");
        }
    }

    // burn-in, throw some user defined number of grains
    // no avalanche recording
    public void burnin(int drops) {
        for (int i = 0; i < drops; i++) {
            this.sandpile.dropGrainAndRelax();
        }
    }
    
    // class method to simulate the grid and record avalanche sizes
    public int[] runDrops(int drops) {
        int[] steps = new int[drops];
        for (int i = 0; i< drops; i++) {
            steps[i] = this.sandpile.dropGrainAndRelax();
        }

        return steps;
    }
    
    // equilibrate the sandpile to reach a critical state
    public void equilibrate(double tol, int m) {
        int[] windowDrops = runDrops(m);
        double prevZeroFreq;
        long zeros = 0L;

        zeros = Arrays.stream(windowDrops).filter(v -> v == 0).count();
        prevZeroFreq = (double) zeros / m;

        double diff = Double.MAX_VALUE;
        while (diff > tol) {
            windowDrops = runDrops(m);
            double currZeroFreq;
            zeros = Arrays.stream(windowDrops).filter(v -> v == 0).count();

            currZeroFreq = (double) zeros / m;
            diff = currZeroFreq - prevZeroFreq;
            diff = Math.abs(diff);
            prevZeroFreq = currZeroFreq;
        }
    }

    public int[] record(int samples) {
        return runDrops(samples);
    }

    // build a Stats class helper
    public static class Stats {
        public final long count;
        public final long sum;
        public final int min;
        public final int max;
        public final double mean;

        public Stats(int[] data) {
            if (data == null || data.length == 0) {
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

            this.count = data.length;
            this.sum = s;
            this.min = mn;
            this.max = mx;
            this.mean = s / (double) count;
        }
    }



    // run the simulation
    public static void main(String[] args) {
        SandpileSimulation newSim = new SandpileSimulation("unstable", "Random", 42L);
        
        newSim.makeSandpile(100);
        newSim.initialize();
        newSim.burnin(1000);
        newSim.equilibrate(0.001, 10000);

        // record
        int[] avalanches = newSim.record(1000000);

        SandpileStatistics st = new SandpileStatistics(avalanches);
        System.out.printf("Avalance sizes: count=%d, min=%d, max=%d, mean=%.4f%n", st.count, st.min, st.max, st.mean);

        // fraction of zero avalanche  drops (no toppling)
        long zeros = Arrays.stream(avalanches).filter(v -> v == 0).count();
        System.out.printf("Zero-size avalanches: %d (%.2f%%)%n", zeros, 100.0 * zeros / Math.max(1, st.count));

        st.binData();

        st.binnedData.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> System.out.println(e.getKey() + " = " + e.getValue()));

        System.out.println();

        st.binnedFreqs.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> System.out.println(e.getKey() + " = " + e.getValue()));

        System.exit(0);


        st.counts.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> System.out.println(e.getKey() + " = " + e.getValue()));
    }
}





/**

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

*/
