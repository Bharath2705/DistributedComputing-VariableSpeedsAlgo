
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

/*
    Class for each process to execute the variable speeds algorithm
 */
class ChildThread extends Thread {
    int uid;                                        // UID of each process
    private boolean isLeader;
    int minUid;                                     // Minimum UID seen by the current process until the current round
    private int incomingUid = -1;
    private ChildThread clockwiseNeighbor;          // Unidirectional neighbor of each process
    private CyclicBarrier childBarrier;             // to make threads wait for each other
    private volatile boolean isLeaderElected;
    private boolean waitForToken;
    private int currentRound;
    private int roundMinTokenEntered;

    ChildThread(int uid, CyclicBarrier childBarrier) {
        this.uid = uid;
        this.minUid = uid;      // setting minimum uid to it's own uid
        this.childBarrier = childBarrier;
        this.currentRound = 0;
        this.roundMinTokenEntered = 0;
    }

    /*
        compares the UID's and sends the minimum UID token to the neighbor
     */
    private void sendMessage() {
        //elects the leader after n*2^minUID rounds
        if(minUid == clockwiseNeighbor.uid)
        {
            clockwiseNeighbor.isLeader = true;
            return;
        }
        // sends the minUID to the neighbor
        else if(minUid < clockwiseNeighbor.minUid) {
            System.out.println("UID "+uid+" - Sending token "+minUid+" to "+clockwiseNeighbor.uid);
            clockwiseNeighbor.minUid = minUid;
            clockwiseNeighbor.roundMinTokenEntered = currentRound;
            clockwiseNeighbor.incomingUid = minUid;
            clockwiseNeighbor.waitForToken = false;
        }
        if(this.currentRound != this.roundMinTokenEntered)
            this.waitForToken = true;
    }

    /*
        Sets the neighbor of the current process
     */
    public void setClockwiseNeighbor(ChildThread neighbor) {
        clockwiseNeighbor = neighbor;
    }

    /*
        Sets the boolean variable to true if any process is elected as leader
     */
    public void setIsLeaderElected() {
        this.isLeaderElected = true;
    }

    /*
        Sets the current round to keep track of the number of rounds
     */
    public void setCurrentRound(int round) {
        this.currentRound = round;
    }

    /*
        Checks if the current process is in the valid round to send token to its neighbor
     */
    private boolean canSendMessage() {
        return this.currentRound == getRoundMinTokenEntered() + (int)(Math.pow(2, minUid));
    }

    private int getRoundMinTokenEntered() {
        return this.roundMinTokenEntered;
    }

    /*
        Thread enters its running state
     */
    public void run() {
        try {
            while(!isLeaderElected) {
                if(!waitForToken) {
                    if(incomingUid == uid) {
                        this.isLeader = true;
                    } else {
                        if(canSendMessage()) {
                            sendMessage();
                        }
                    }
                }
                childBarrier.await();
            }
            if(isLeader) {
                System.out.println("UID "+uid+" - I am the leader");
            } else {
                System.out.println("UID "+uid+" - I am not the leader");
            }
        } catch(Exception e) {
            System.out.println("Exception occurred "+e);
        }
    }

    /*
        Checks if the process is a leader
     */
    public boolean isLeader() {
        return isLeader;
    }
}

/*
    Master Thread class that controls the child threads execution
 */
class MasterThread extends Thread {
    private int n;                                      // Number of processes/threads
    private int uids[];                                 // Process uid's
    private volatile boolean isLeaderElected;
    private int roundsCompleted;                        // Rounds completed until current time
    private List<ChildThread> childThreads;             // List of child threads

    MasterThread() {
        roundsCompleted = 0;
    }

    /*
        Method executed after n threads enter into the cyclic barrier in each round
     */
    public void updateRoundInformation() {
        System.out.println("Round number "+roundsCompleted+" completed");
        roundsCompleted++;
        for(ChildThread c: childThreads) {
            isLeaderElected = isLeaderElected || c.isLeader();
            c.setCurrentRound(roundsCompleted);
        }

        // Informing each thread about the leader
        if(isLeaderElected) {
            for(ChildThread c: childThreads)
                c.setIsLeaderElected();

        }
    }

    /*
        run method of master thread invoked by main class
     */
    public void run() {
        try {
            // Reading file input
            File file = new File("C://Users//rbkch//IdeaProjects//VariableSpeeds//src//input.txt");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String st;
            // flag for reading file input
            boolean flag = true;
            while ((st = br.readLine()) != null)
            {
                if(flag) {
                    flag = false;
                    n = Integer.parseInt(st);
                } else {
                    uids = new int[n];
                    String[] temp = st.split(" ");
                    if(temp.length < n) throw new Exception(n+" UIDs are expected");
                    for(int i = 0; i < n; i++) {
                        uids[i] = Integer.parseInt(temp[i]);
                    }
                }
            }

            CyclicBarrier childBarrier = new CyclicBarrier(n, this::updateRoundInformation);
            childThreads = new ArrayList<>();
            // Creating child threads
            for(int i = 0; i < n; i++) {
                ChildThread c = new ChildThread(uids[i], childBarrier);
                childThreads.add(c);
            }

            // Sets neighbor to each child thread
            for(int i = 0; i < n; i++) {
                ChildThread c = childThreads.get(i);
                c.setClockwiseNeighbor(childThreads.get((i+1)%n));
            }

            // Invoking each thread
            for(ChildThread c: childThreads)
                c.start();

            // Loop until leader is not elected to keep the threads alive
            while(!isLeaderElected){  }

        } catch(Exception e) {
            System.out.println("Exception occurred "+e);
        }
    }

}


// Main Class
public class Main {
    public static void main(String[] args) {
        // Creating the master thread
        MasterThread m = new MasterThread();
        m.start();
    }
}