import java.util.*;

public class ExperimentRunner {
    public static void main(String[] args) {
        final int DROPS = 1000000;
        List<Integer> avalancheSizes;
        int[] GridSizes = {8, 16, 32, 64, 128, 256};
        long seed;

        System.out.println("=== Experiment Runner ===");

        for (int n : GridSizes) {
            System.out.printf("%4s %d%n",">", n);
            seed = System.nanoTime();
            SandpileSimulation.Sandpile sp = new SandpileSimulation.Sandpile(n, seed);

            List<Integer> avalanches = sp.runDrops(DROPS);

            SandpileSimulation.Stats st = new SandpileSimulation.Stats(avalanches);
            System.out.printf("%8sAvalance sizes:, max=%7d, mean=%10.4f%n",
            "", st.max, st.mean);

            // fraction of zero avalanche  drops (no toppling)
            long zeros = avalanches.stream().filter(v -> v == 0).count();
            System.out.printf("%8sZero-size avalanches: %6d (%.2f%%)%n",
                "", zeros, 100.0 * zeros / Math.max(1, st.count));
        }
    }
}
