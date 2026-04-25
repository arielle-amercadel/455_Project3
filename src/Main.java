public class Main {

    public static void main(String[] args) {
        int algorithm = -1;
        int quantum = -1;
        int cores = 1; //default

        try {
            System.out.print("User inputted: ");
            for (String arg : args) {
                System.out.print(arg + " ");
            }
            System.out.println();

            for (int i = 0; i < args.length; i++) {

                switch (args[i]) {
                    case "-S":
                        // if there is no parameter, exit
                        if (i + 1 >= args.length) {
                            error("Missing parameter for -S");
                        }

                        // parse for algorithm
                        algorithm = Integer.parseInt(args[++i]);

                        if (algorithm < 1 || algorithm > 4) {
                            error("Invalid parameter for -S. Algorithm must be between 1 and 4.");
                        }

                        // if algorithm selected is round-robin
                        if (algorithm == 2) {
                            if (i + 1 >= args.length) {
                                error("Missing parameter for Round-Robin algorithm. Quantum must be between 2 and 10.");
                            }

                            // parse for quantum value
                            quantum = Integer.parseInt(args[++i]);

                            // check for valid quantum value
                            if (quantum < 2 || quantum > 10) {
                                error("Quantum must be between 2 and 10.");
                            }
                        }
                        break;

                    case "-C":
                        if (i + 1 >= args.length) {
                            error("Missing parameter for -C");
                        }

                        // parse for core value
                        cores = Integer.parseInt(args[++i]);

                        // check for valid core value
                        if (cores < 1 || cores > 4) {
                            error("Invalid parameter for -C. Cores must be between 1 and 4.");
                        }
                        break;

                    default:
                        if (algorithm == -1) {
                            error("Scheduling argument (-S) is required");
                        }
                        error("Invalid argument: " + args[i]);
                        break;
                }
            }

            if (algorithm == -1) {
                error("Scheduling argument (-S) is required");
            }

            if (algorithm == 4 && cores > 1) {
                error("Algorithm 4 (PSJF) cannot be used with multiple CPU cores");
            }

            /*
            System.out.println("Algorithm: " + algorithm);
            System.out.println("Quantum: " + quantum);
            System.out.println("Cores: " + cores);
            */

            Task1.main(algorithm,cores,quantum);

        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    public static void error(String msg) {
        System.err.println("Error: " + msg);
        System.exit(1);
    }

}
