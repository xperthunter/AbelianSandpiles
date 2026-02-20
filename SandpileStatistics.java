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
    int maxExponent;
    public void binData() {
        double totalPseudo = (double) this.count - this.counts.get(0);
        double maxSize = (double) this.max;

        maxExponent = (int) (Math.log(maxSize) / Math.log(2));
        for (int i = 0; i <= maxExponent; i++ ) {
            binnedData.put(i, 0);
            binnedFreqs.put((double) i, 0.0);
        }

        double denominator;
        for (int avs : counts.keySet()) {
            if(avs == 0){ continue;}

            int ex = (int) (Math.log((double) avs) / Math.log(2));

            denominator = Math.pow(2.0, (double) ex + 0.5) * totalPseudo;

            binnedData.put(ex, binnedData.get(ex) + counts.get(avs));
            binnedFreqs.put((double) ex, ((double) binnedData.get(ex)) / denominator);
        }
    }

    public double computeScalingExponent() {

        double x1;
        double y1;
        double x2;
        double y2;
        double scaling;

        x1 = Math.pow(2.0, 3.5);
        y1 = binnedFreqs.get(2.0);

        x2 = ((double) maxExponent) - 3.0;
        y2 = binnedFreqs.get(x2);
        x2 = Math.pow(2.0, x2);

        scaling = Math.log10(y2) - Math.log10(y1);
        scaling = scaling / (Math.log10(x2) - Math.log10(x1));

        return scaling;
    }

    public static void main (String[] args) {
        System.out.println("hello");
    }
}
