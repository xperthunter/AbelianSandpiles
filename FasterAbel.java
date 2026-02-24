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
	* Implementing Copilot recommendations for speed-ups
    *
    * Compile: javac SandpileSimulation.java
    * Run:     java SandpileSimulation
*/

public class FasterAbel {

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
    public FasterAbel() {
        this("cold", "Random", 42L);
    }

    // constructor with args
    public FasterAbel(String start, String rngMethod, long seed) {
        if (!start.equals("hot") && !start.equals("cold") && !start.equals("unstable")) {
            throw new IllegalArgumentException("start must be [hot], [cold], or [unstable]");
        }

        this.start = start;
        this.rngMethod = rngMethod;
        this.seed  = seed;

        this.rng = RandomGeneratorFactory.of(rngMethod).create(seed);
    }

    // sandpile class -- contains methods that alter the sandpile state
    public static class FastSandpile {
        public final int n;                     // grid size (n x n)
        private final int stride;                   // n + 2 (ghost border)
        private final int size;					// stride * stride
        private final int[] z;					// flat grid with ghost border
        private final boolean[] interior;		// interior mask
        private final byte[] inQ;				// 0/1 flags per cell
        private final int[] q;					// circular queue for indices
        private int head = 0, tail = 0;
        private final int offN, offS, offE, offW;
        RandomGenerator rng;

        // constructor
        public FastSandpile(int n, RandomGenerator rng) {
            if (n <= 0) throw new IllegalArgumentException("n must be positive");
            this.n = n;
            this.stride = n + 2;
            this.size = stride * stride;
            this.z = new int[size];
            this.interior = new boolean[size];
            this.inQ = new byte[size];
            this.q = new int[size]; // safe upper bound
            this.rng = rng;
            
            // precompute neighbor offsets
            this.offN = -stride;
            this.offS = +stride;
            this.offE = +1;
            this.offW= -1;
            
            // make interior mask
            for (int i = 1; i <= n; i++) {
            	int base = i * stride;
            	for (int j = 1; j <= n; j++) {
            		interior[base + j] = true;
            	}
            }
        }
        
        private int idx(int i, int j) { return i * stride + j; }
        
        public int size() {return n; }

        public int[][] snapshot() {
            int[][] copy = new int[n][n];
            for (int i = 0; i < n; i++) System.arraycopy(z[i], 0, copy[i], 0, n);
            return copy;
        }
        
        public void reset() {
        	java.util.Arrays.fill(z, 0);
        	java.util.Arrays.fill(inQ, (byte) 0);
        	head = tail = 0;
        }
        
        // grid set to all zeros
        public void coldStart() {
            reset();
        }
		
		// cells set to 0,1,2,3 randomly (uniformly)
        public void hotStart() {
            reset();
            for (int i = 0; i <= n; i++) {
                int base = i * stride;
                for (int j = 0; j < n; j++) {
                	z[base + j] = rng.nextInt(4);
                }
            }
        }
        
        // every cell set to 3
        public void unstableStart() {
        	reset();
        	System.out.println(stride);
            for (int i = 0; i <= n; i++) {
            	int base = i * stride;
                java.util.Arrays.fill(z, base + 1, base + 1 + n, 3);
            }
        }
        
        private void qClear() { head = tail = 0; }
        private void qAdd(int v) {q[tail++] = v; if (tail == q.length) tail = 0;}
        private int qPoll() { int v = q[head++]; if (head == q.length) head = 0; return v;}
        private boolean qIsEmpty() { return head == tail; }
        public int dropGrainAndRelax() {
            int i = 1 + rng.nextInt(n);
            int j = 1 + rng.nextInt(n);
            int r = idx(i, j);
            int h = ++z[r];
            
            if (h < 4) return 0;
            
            qClear();
            qAdd(r);
            inQ[r] = 1;
            
            int avalancheTopples = 0;

            while(!qIsEmpty()) {
                int c = qPoll();
                inQ[c] = 0;
                
                int hc = z[c];
                if (hc < 4) continue;
                
                // batch topples
                int times = hc >>> 2;             // hc / 4
                z[c] = hc - (times << 2);         // hc % 4
                avalancheTopples += times;
                
                
                // neightbor indices
                int nN = c + offN;
                int nS = c + offS;
                int nE = c + offE;
                int nW = c + offW;
                
//                 System.out.println(c+" "+hc+" "+times+" "+stride);
//                 System.out.println(nN);
//                 System.out.println(nS);
//                 System.out.println(nE);
//                 System.out.println(nW);
                
                int t;
                
                // north
                t = z[nN] += times;
                if (interior[nN] && t >= 4 && inQ[nN] == 0){
                	qAdd(nN);
                	inQ[nN] = 1;
                }
                
                // south
                t = z[nS] += times;
                if (interior[nS] && t >= 4 && inQ[nN] == 0){
                	qAdd(nS);
                	inQ[nS] = 1;
                }
                
                // east
                t = z[nE] += times;
                if (interior[nE] && t >= 4 && inQ[nE] == 0){
                	qAdd(nE);
                	inQ[nE] = 1;
                }
                
                // west
                t = z[nW] += times;
                if (interior[nW] && t >= 4 && inQ[nW] == 0){
                	qAdd(nW);
                	inQ[nW] = 1;
                }
            }

            return avalancheTopples;
        }
    }
    
    // make a sandpile in the simulation
    private FastSandpile sandpile;
    public void makeSandpile (int n) {
        this.sandpile = new FastSandpile(n, this.rng);
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
        
        newSim.makeSandpile(8);
        newSim.initialize();
        newSim.burnin(10000);
        newSim.equilibrate(0.001, 20000);

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

        double exp = st.computeScalingExponent();
        System.out.printf("%n%f%n", exp);


        System.exit(0);
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
