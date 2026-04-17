//Task 1: FCFS, RR, NSJF, PSJF for Single Core CPU
// Arielle Mercadel
import java.util.*;

class Task {

    int id; //ID number
    int maxBurst; //Total burst length
    int currentBurst; //Current burst length
    int remainingTime; //Time left on CPU
    int arrivalTime; //Time when process arrives
    boolean isFinished; //Checks if process finished

    //Constructor to initialize task with ID and burst time
    public Task(int id, int burst) {
        this.id = id; //Assign ID
        this.maxBurst = burst; //Set burst time
        this.currentBurst = 0; //No CPU time
        this.remainingTime = burst; //Full burst remaining
        this.isFinished = false; //Not completed
        this.arrivalTime = (int)(Math.random() * 5); //Random arrival time
    }
}

public class Task1 {
    static List<Task> readyQueue = new ArrayList<>(); //ready queue
    static int cpuId = 0; //CPU ID
    static int quantum = 5; //Time quantum for RR

    public static void main(String[] args) {
        int algorithm = 4; //Algorithm testing

        //Command Line
        //if (args.length >= 2 && args[0].equals("-S")) {
            //try {
                //algorithm = Integer.parseInt(args[1]);
            //} catch (Exception e) {}
       // }

        //Prints algorithm chosen
        if (algorithm == 1)
            System.out.println("Scheduler Algorithm Select: FCFS");
        else if (algorithm == 2)
            System.out.println("Scheduler Algorithm Select: Round Robin. Time Quantum = " + quantum);
        else if (algorithm == 3)
            System.out.println("Scheduler Algorithm Select: Non Preemptive - Shortest Job First");
        else if (algorithm == 4)
            System.out.println("Scheduler Algorithm Select: Preemptive - Shortest Job First");

        //Random generator for burst times
        Random rand = new Random();

        //Number of threads to simulate
        int T = rand.nextInt(25)+1;
        System.out.println("# threads = " + T);

        //List of all processes
        List<Task> processes = new ArrayList<>();

        //Create tasks
        for (int i = 0; i < T; i++) {
            int burst = rand.nextInt(50) + 1; //random burst b/t 1-50
            Task t = new Task(i, burst); //
            //Print creation message
            System.out.println("Main thread     | Creating process thread " + i);
            processes.add(t);
        }

        //Ready Queue
        readyQueue.clear(); //Clear any previous data
        readyQueue.addAll(processes); //Add all generated processes in queue
        printReadyQueue(); //Print ready queue

        //Dispatcher
        System.out.println("Main thread     | Forking dispatcher 0");
        System.out.println("Dispatcher 0    | Using CPU 0");
        System.out.println("Dispatcher 0    | Now releasing dispatchers.\n");

        //Run selected algorithm
        if (algorithm == 1) { //FCFS
            System.out.println("Dispatcher 0    | Running FCFS algorithm\n");
            runFCFS();
        }
        else if (algorithm == 2) { //RR
            System.out.println("Dispatcher 0    | Running RR algorithm, Time Quantum = " + quantum + "\n");
            runRR();
        }
        else if (algorithm == 3) { //NSJF
            System.out.println("Dispatcher 0    | Running Non Preemptive - Shortest Job First\n");
            runNSJF();
        }
        else if (algorithm == 4) { //PSJF
            System.out.println("Dispatcher 0    | Running Preemptive - Shortest Job First\n");
            runPSJF(processes);
        }
        //End simulation
        System.out.println("Main thread     | Exiting.");
    }

    //First Come, First Served (FCFS)
    public static void runFCFS() {
        for (Task t : readyQueue) {
            //Dispatcher selects next process
            System.out.println("Dispatcher 0    | Running process " + t.id);
            runCPU(t, t.remainingTime); //Run process for full time
        }
    }

    //Round Robin
    public static void runRR() {
        //Create queue
        Queue<Task> queue = new LinkedList<>(readyQueue);
        //Run until all processes are completed
        while (!queue.isEmpty()) {
            Task t = queue.poll(); //Get next process
            System.out.println("Dispatcher 0    | Running process " + t.id);
            //See how long the process can run
            int timeWindow = Math.min(quantum, t.remainingTime);
            //Execute process for selected time
            runCPU(t, timeWindow);

            //If process not finished, add it back to the queue
            if (!t.isFinished) {
                queue.add(t);
            }
        }
    }

    //Non-Preemptive Shortest Job First (NSJF)
    public static void runNSJF() {
        //Create copy of queue
        List<Task> queue = new ArrayList<>(readyQueue);

        //Run until all processes have been scheduled
        while (!queue.isEmpty()) {
            //Set first process as shortest burst
            Task shortest = queue.get(0);
            //Look for process with shortest burst time
            for (Task t : queue) {
                if (t.maxBurst < shortest.maxBurst) {
                    shortest = t;
                }
            }
            //Remove shortest job from queue
            queue.remove(shortest);
            //Dispatcher selects shortest job
            System.out.println("Dispatcher 0    | Running process " + shortest.id);
            //Run selected process
            runCPU(shortest, shortest.remainingTime);
        }
    }

    //Preemptive Shortest Job First (PSJF)
    public static void runPSJF(List<Task> processes) {
        int time = 0; //clock

        //List of processes ready to run
        List<Task> ready = new ArrayList<>();
        //List of processes that have not arrived yet
        List<Task> notArrived = new ArrayList<>(processes);

        //Current running process
        Task current = null;

        //Run until everything is finished
        while (!ready.isEmpty() || !notArrived.isEmpty() || current != null) {

            //Check for new arrivals
            boolean arrived = false;
            Iterator<Task> it = notArrived.iterator();
            //Move newly arrived tasks into ready queue
            while (it.hasNext()) {
                Task t = it.next();
                if (t.arrivalTime <= time) {
                    ready.add(t);
                    it.remove();
                    //Print arrival
                    System.out.println("Dispatcher 0    | Process " + t.id + " arrived");
                    arrived = true;
                }
            }
            //If a process arrived, print updated ready queue
            if (arrived) {
                System.out.println("--------------- Ready Queue ---------------");
                for (Task t : ready) {
                    System.out.println("ID:" + t.id + ", Max Burst:" + t.maxBurst + ", Current Burst:" + t.currentBurst);
                }
                System.out.println("-------------------------------------------\n");
            }

            //Select shortest process
            Task shortest = null;
            //Search for process with smallest remainin time
            for (Task t : ready) {
                if (shortest == null || t.remainingTime < shortest.remainingTime) shortest = t;
            }
            //Preemption
            if (current == null) {
                //If no process running, start shortest process
                if (shortest != null) {
                    ready.remove(shortest);
                    current = shortest;
                    //Print updated ready queue after decision
                    System.out.println("Dispatcher 0    | Running process " + current.id);
                    System.out.println("--------------- Ready Queue ---------------");
                    for (Task t : ready) {
                        System.out.println("ID:" + t.id + ", Max Burst:" + t.maxBurst + ", Current Burst:" + t.currentBurst);
                    }
                    System.out.println("-------------------------------------------\n");
                }
            } else {
                //If a process is running, check if new process is shorter
                if (shortest != null && shortest.remainingTime < current.remainingTime) {
                    //Preempt current process if needed
                    if (current.remainingTime > 0) {
                        ready.add(current);
                    }
                    //Switch to shorter process
                    ready.remove(shortest);
                    current = shortest;

                    System.out.println("Dispatcher 0    | Running process " + current.id);
                    //Reprint ready queue
                    System.out.println("--------------- Ready Queue ---------------");
                    for (Task t : ready) {
                        System.out.println("ID:" + t.id + ", Max Burst:" + t.maxBurst + ", Current Burst:" + t.currentBurst);
                    }
                    System.out.println("-------------------------------------------\n");
                }
            }

            //If no process is running, then CPU is idle
            if (current == null) {
                time++;
                continue;
            }

            //Run one CPU cycle
            current.currentBurst++; //increment burst time
            current.remainingTime--; //decrease time left
            System.out.println("Proc. Thread " + current.id +
                    "  | Using CPU 0; On burst " + current.currentBurst + ".");

            //Check if process if finished
            if (current.remainingTime <= 0) {
                current.isFinished = true;
                current = null;
            }
            //Advance time
            time++;
        }
    }
    //CPU simulation
    public static void runCPU(Task t, int timeWindow) {
        //CPU does not run longer than time left
        if (timeWindow > t.remainingTime) {
            timeWindow = t.remainingTime;
        }
        //Print execution details
        System.out.println("Proc. Thread " + t.id + "  | On CPU: MB=" + t.maxBurst +
                ", CB=" + t.currentBurst + ", BT=" + timeWindow + ", BG:=" + timeWindow);
        //CPU execution
        for (int i = 1; i <= timeWindow; i++) {
            t.currentBurst++; //increment burst time
            t.remainingTime--; //decrement time lef
            //print progress for each cycle
            System.out.println("Proc. Thread " + t.id + "  | Using CPU " + cpuId +
                    "; On burst " + t.currentBurst + ".");
        }
        //If no time left, process is finished
        if (t.remainingTime <= 0) {
            t.isFinished = true;
        }
        System.out.println();
    }

    //Print ready queue
    public static void printReadyQueue() {
        //Header
        System.out.println("\n--------------- Ready Queue ---------------");
        //Print ID, maxBurst, and currentBurst
        for (Task t : readyQueue) {
            System.out.println("ID:" + t.id + ", Max Burst:" + t.maxBurst +
                    ", Current Burst:" + t.currentBurst);
        }
        System.out.println("-------------------------------------------\n");
    }
}