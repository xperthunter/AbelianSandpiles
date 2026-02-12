import java util.*;

public class SandpileStatistics {

    public static class SandpileRunner {
        private String start;
        public Runner(String start) {
            if (start == "hot") {
            }
            else if (start == "cold") {
            }
            else {
                throw new IllegalArgumentException("start must be hot or cold");
            }

            this.start = start;
        }
    }

    public static void main(String[] args) {
        System.out.println("hello");
        SandpileRunner runner = new SandpileRunner("hot");
        System.out.println(runner.start);
}
