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
    }

    public static HashMap<Integer, Double

    /**
    public static float computeScalingExponent() {
        // turn everything into log-lo

    }
    */

    public static void main (String[] args) {
        System.out.println("hello");
    }
}
