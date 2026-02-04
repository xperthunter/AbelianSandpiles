import java.util.*;

public class ExperimentRunner {
    public static void main(String[] args) {
        final int DROPS = 1000000;
        List<Integer> avalancheSizes;
        int[] GridSizes = {16, 32, 64, 128};
        long seed;

        System.out.println("=== Experiment Runner ===");

        for (int n : GridSizes) {
            System.out.println("> " + n);
            seed = System.nanoTime();
            SandpileSimulation.Sandpile sp = new SandpileSimulation.Sandpile(n, seed);

            List<Integer> avalanches = sp.runDrops(DROPS);

            SandpileSimulation.Stats st = new SandpileSimulation.Stats(avalanches);
            System.out.printf("Avalance sizes: count=%d, min=%d, max=%d, mean=%.4f%n",
            st.count, st.min, st.max, st.mean);

            // fraction of zero avalanche  drops (no toppling)
            long zeros = avalanches.stream().filter(v -> v == 0).count();
            System.out.printf("Zero-size avalanches: %d (%.2f%%)%n",
                zeros, 100.0 * zeros / Math.max(1, st.count));
        }
    }
}
