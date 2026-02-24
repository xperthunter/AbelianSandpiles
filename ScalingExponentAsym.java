import java.security.SecureRandom;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public class ScalingExponentAsym {
    public static double[] stats(double[] data) {
        double sum = 0.0;
        double mean = 0.0;
        double sd = 0.0;

        for (double ee : data){
            sum = sum + ee;
        }

        mean = sum / data.length;

        sum = 0.0;
        for (double ee : data) {
            sum = Math.pow(ee - mean, 2.0);
        }

        sd = sum / data.length;
        sd = Math.pow(sum, 0.5);

        return new double[] {mean, sd};
    }

    public static void main(String[] args) {
        final int RESAMPLES = 1;
        int[] GridSizes = {128, 256, 512};
        long sr;
        int[] avalanches;
        double[] expos = new double[RESAMPLES];
        SecureRandom seed = new SecureRandom();

        System.out.printf("%n%10s=== Tau asymptotics ===%n", "");

        for (int n : GridSizes ){
            for (int i = 0; i < RESAMPLES; i++){
                sr = seed.nextLong();
                FasterAbel sim = new FasterAbel("hot", "SplittableRandom", sr);
                
                sim.makeSandpile(n);
                sim.initialize();
                sim.burnin(1000);
                sim.equilibrate(0.001, 10000);

                // record
                avalanches = sim.record(1000000);
                
                // statistics
                SandpileStatistics st = new SandpileStatistics(avalanches);

                // bin data
                st.binData();

                // exponent
                expos[i] = st.computeScalingExponent();
            }
            double [] res = stats(expos);
            System.out.printf("%5sSize: %-4d avg: %6.4f  +/-  %6.4f%n", "", n, res[0], res[1]);
        }



        System.out.println("");
    }
}
