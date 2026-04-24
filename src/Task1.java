//Task 1: FCFS, RR, NSJF, PSJF for Single Core CPU
//Arielle Mercadel
import java.util.*;
import java.util.concurrent.Semaphore;

class Task extends Thread {
    int id; // ID number
    int maxBurst; // Total burst length
    int currentBurst; // Current burst length
    int remainingTime; // Time left on CPU
    int arrivalTime; // Time when process arrives
    boolean isFinished; // Checks if process finished
    Semaphore sem; // Semaphore to control execution


    public Task(int id, int burst, Semaphore sem) {
        this.id = id;
        this.maxBurst = burst;
        this.currentBurst = 0;
        this.remainingTime = burst;
        this.arrivalTime = (int)(Math.random() * 5);
        this.isFinished = false;
        this.sem = sem;
    }

    //Thread logic
    public void run() {
        while (remainingTime > 0) {
            try {
                sem.acquire(); // Wait for CPU permission
                if (remainingTime > 0) {
                    currentBurst++; //Increase executed time
                    remainingTime--; //Decrease remaining time
                    //Print progress
                    System.out.println("Proc. Thread " + id + "  | Using CPU 0; On burst " + currentBurst + ".");
                    if (remainingTime <= 0) { //Mark finished if done
                        isFinished = true;
                    }
                }
            } catch (InterruptedException e) {
                // Used for preemption in PSJF
                System.out.println("Proc. Thread " + id + "  | Preempted, remaining " + remainingTime);
            }
        }
    }
}

//Dispatcher thread
class Dispatcher extends Thread {
    List<Task> readyQueue; //Ready queue
    int algorithm; //Selected algorithm
    int quantum; //Time quantum for RR
    List<Task> allTasks; //Tasks for PSJf
    Semaphore cpuSem;

    //Initialize dispatcher
    public Dispatcher(List<Task> readyQueue, int algorithm, int quantum, List<Task> allTasks, Semaphore cpuSem) {
        this.readyQueue = readyQueue;
        this.algorithm = algorithm;
        this.quantum = quantum;
        this.allTasks = allTasks;
        this.cpuSem = cpuSem;
    }

    //Select algorithm
    public void run() {
        if (algorithm == 1) {
            runFCFS();
        } else if (algorithm == 2) {
            runRR();
        } else if (algorithm == 3) {
            runNSJF();
        } else if (algorithm == 4) {
            runPSJF();
        }
    }

    //First Come First Serve
    private void runFCFS() {
        synchronized (readyQueue) { //Protect shared queue
            while (!readyQueue.isEmpty()) {
                Task t = readyQueue.remove(0); //Get first task
                System.out.println("Dispatcher 0    | Running process " + t.id);
                runCPU(t, t.remainingTime);
            }
        }
    }

    //Round Robin
    private void runRR() {
        while (true) {
            Task t = null;
            //Get next task
            synchronized (readyQueue) { //Protect queue
                if (!readyQueue.isEmpty()) {
                    t = readyQueue.remove(0); //Get next task
                }
            }
            if (t == null) break; //Exit if no tasks
            System.out.println("Dispatcher 0    | Running process " + t.id);
            //Get execution time
            int timeWindow = Math.min(quantum, t.remainingTime);
            runCPU(t, timeWindow); //Run for quantum
            //Add again if not finished
            if (!t.isFinished) {
                synchronized (readyQueue) {
                    readyQueue.add(t);
                }
            }
        }
    }

    //Non-preemptive Shortest Job First
    private void runNSJF() {
        while (true) {
            Task shortest = null;
            //Find shortest remaining task
            synchronized (readyQueue) {
                if (!readyQueue.isEmpty()) {
                    shortest = readyQueue.get(0);
                    for (Task task : readyQueue) {
                        if (task.remainingTime < shortest.remainingTime) {
                            shortest = task;
                        }
                    }
                    readyQueue.remove(shortest); //Remove selected task
                }
            }
            if (shortest == null) break; //Exit if empty
            System.out.println("Dispatcher 0    | Running process " + shortest.id);
            runCPU(shortest, shortest.remainingTime);
        }
    }

    //Preemptive Shortest Job First
    private void runPSJF() {
        int time = 0; //Simulation clock
        List<Task> notArrived = new ArrayList<>(allTasks);
        Task current = null; //Current running task

        while (!readyQueue.isEmpty() || !notArrived.isEmpty() || current != null) {
            // Check for newly arrived tasks
            synchronized (notArrived) {
                Iterator<Task> it = notArrived.iterator();
                while (it.hasNext()) {
                    Task t = it.next();
                    if (t.arrivalTime <= time) {
                        synchronized (readyQueue) {
                            readyQueue.add(t);
                        }
                        it.remove(); //Remove from future list
                        System.out.println("Dispatcher 0    | Process " + t.id + " arrived");
                        printReadyQueue();
                    }
                }
            }

            // Find shortest task in ready queue
            Task shortest = null;
            synchronized (readyQueue) {
                for (Task t : readyQueue) {
                    if (shortest == null || t.remainingTime < shortest.remainingTime) {
                        shortest = t;
                    }
                }
            }
            //Decide whether to start or preempt
            if (current == null) {
                if (shortest != null) {
                    synchronized (readyQueue) {
                        readyQueue.remove(shortest);
                    }
                    current = shortest;
                    System.out.println("Dispatcher 0    | Running process " + current.id);
                }
            } else {
                if (shortest != null && shortest.remainingTime < current.remainingTime) {
                    current.interrupt(); // Preempt
                    synchronized (readyQueue) {
                        readyQueue.add(current); //Add old task back
                    }
                    synchronized (readyQueue) {
                        readyQueue.remove(shortest);
                    }
                    current = shortest;
                    System.out.println("Dispatcher 0    | Running process " + current.id);
                }
            }

            if (current != null) {
                // Run one cycle
                current.sem.release();
                try {
                    Thread.sleep(10); // Simulate time
                } catch (InterruptedException e) {}
                if (current.isFinished) {
                    current = null;
                }
            }
            time++;
        }
    }

    //CPU
    private void runCPU(Task t, int timeWindow) {
        System.out.println("Proc. Thread " + t.id + "  | On CPU: MB=" + t.maxBurst +
                ", CB=" + t.currentBurst + ", BT=" + timeWindow + ", BG=" + timeWindow);
        for (int i = 0; i < timeWindow; i++) {
            t.sem.release(); //Allow one cycle
            try {
                Thread.sleep(10); // Simulate cycle time
            } catch (InterruptedException e) {}
        }
        System.out.println();
    }

    //Print ready queue
    private void printReadyQueue() {
        synchronized (readyQueue) {
            System.out.println("--------------- Ready Queue ---------------");
            for (Task t : readyQueue) {
                System.out.println("ID:" + t.id + ", Max Burst:" + t.maxBurst + ", Current Burst:" + t.currentBurst);
            }
            System.out.println("-------------------------------------------\n");
        }
    }
}

public class Task1 {
    static List<Task> readyQueue = Collections.synchronizedList(new ArrayList<>());
    static int cpuId = 0;
    static int quantum = 5;

    public static void main(String[] args) {
        int algorithm = 4; // algorithm testing

        //Print selected algorithm
        if (algorithm == 1)
            System.out.println("Scheduler Algorithm Select: FCFS");
        else if (algorithm == 2)
            System.out.println("Scheduler Algorithm Select: Round Robin. Time Quantum = " + quantum);
        else if (algorithm == 3)
            System.out.println("Scheduler Algorithm Select: Non Preemptive - Shortest Job First");
        else if (algorithm == 4)
            System.out.println("Scheduler Algorithm Select: Preemptive - Shortest Job First");

        Random rand = new Random();
        int T = rand.nextInt(25) + 1; //Number of tasks
        System.out.println("# threads = " + T);

        List<Task> processes = new ArrayList<>(); //Store tasks
        Semaphore cpuSem = new Semaphore(1); //Share CPU

        for (int i = 0; i < T; i++) {
            int burst = rand.nextInt(50) + 1; //Burst time
            Task t = new Task(i, burst, new Semaphore(0));
            System.out.println("Main thread     | Creating process thread " + i);
            processes.add(t); //Add to list
            t.start(); // Start thread
        }

        //Fill queue for not PSJF
        if (algorithm != 4) {
            synchronized (readyQueue) {
                readyQueue.addAll(processes);
            }
            printReadyQueue();
        }

        System.out.println("Main thread     | Forking dispatcher 0");
        System.out.println("Dispatcher 0    | Using CPU 0");
        System.out.println("Dispatcher 0    | Now releasing dispatchers.\n");

        Dispatcher dispatcher = new Dispatcher(readyQueue, algorithm, quantum, processes, cpuSem);
        dispatcher.start(); //Start scheduler

        try {
            dispatcher.join(); // Wait for dispatcher to finish
        } catch (InterruptedException e) {}

        System.out.println("Main thread     | Exiting.");
    }

    public static void printReadyQueue() {
        synchronized (readyQueue) {
            System.out.println("\n--------------- Ready Queue ---------------");
            for (Task t : readyQueue) {
                System.out.println("ID:" + t.id + ", Max Burst:" + t.maxBurst + ", Current Burst:" + t.currentBurst);
            }
            System.out.println("-------------------------------------------\n");
        }
    }
}