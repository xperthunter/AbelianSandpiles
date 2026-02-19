import java.util.*;

/**
public class LinearRegression {
    public final int[] data;
    public LinearRegression(int[] data) {

    }
}
*/

public class SandpileStatistics {
    public final long count;
    public final int min;
    public final int max;
    public final double mean;
    public final int[] data;
    public HashMap<Integer, Double> freqs;
    public HashMap<Integer, Integer> counts;

    public SandpileStatistics(int[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("data argument cannot be empty");
        }

        this.count = data.length;
        this.data = data;

        long sum = 0;
        double incr = 1.0/this.count;
        HashMap<Integer, Double> freqs = new HashMap<Integer, Double>();
        HashMap<Integer, Integer> counts = new HashMap<Integer, Integer>();

        int mn = Integer.MAX_VALUE;
        int mx = Integer.MIN_VALUE;

        for (int v: this.data) {
            sum += v;
            if (v < mn) mn = v;
            if (v > mx) mx = v;

            if (freqs.get(v) == null) {
                freqs.put(v, (double) 0);
                counts.put(v, 0);
            }

            freqs.put(v, freqs.get(v) + incr);
            counts.put(v, counts.get(v) + 1);
        }

        this.min = mn;
        this.max = mx;
        this.mean = sum / (double) count;
        this.freqs = freqs;
        this.counts = counts;

        /**
        counts.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> System.out.println(e.getKey() + " = " + e.getValue()));
        */

    }

    HashMap<Integer, Integer> binnedData = new HashMap<Integer, Integer>();
    HashMap<Double, Double> binnedFreqs = new HashMap<Double, Double>();
    public void binData() {
        double totalPseudo = (double) this.count - this.counts.get(0);
        double maxSize = (double) this.max;
        System.out.println(maxSize);

        int maxExponent = (int) (Math.log(maxSize) / Math.log(2));
        System.out.println(maxExponent);
        //int pscounts = 0;
        for (int i = 0; i <= maxExponent; i++ ) {
            //pscounts = (int) Math.pow(2.0, (double) i);
            //totalPseudo = totalPseudo + pscounts;
            //System.out.printf("size: %d psuedo-counts: %d%n", i, pscounts);
            binnedData.put(i, 0);
            binnedFreqs.put((double) i, 0.0);
        }

        binnedData.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> System.out.println(e.getKey() + " = " + e.getValue()));

        double denominator;
        for (int avs : counts.keySet()) {
            if(avs == 0){ continue;}

            int ex = (int) (Math.log((double) avs) / Math.log(2));
            //System.out.println(ex);
            //System.out.println(avs);

            denominator = Math.pow(2.0, (double) ex + 0.5) * totalPseudo;

            binnedData.put(ex, binnedData.get(ex) + counts.get(avs));
            binnedFreqs.put((double) ex, ((double) binnedData.get(ex)) / denominator);
        }

        for (int ex : binnedData.keySet()) {
            System.out.printf("log-size: %d count: %d%n", ex, binnedData.get(ex));
        }
    }

    /**
    public static float computeScalingExponent() {
        // turn everything into log-lo

    }
    */

    public static void main (String[] args) {
        System.out.println("hello");
    }
}
