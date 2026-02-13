import java util.*;

public class SandpileStatistics {

    public static class SandpileRunner {
        private String start;
        private
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

/**
i want to make a cold start vs hot start
i am thinking about separating the sandpile specific definitions and methods from
the simulation of the sandpile.
can i just put the hot and cold starts in the original sandpile simulation class?
sandpile is the subclass
the random number generator needs to be removed from sandpile class
*/
