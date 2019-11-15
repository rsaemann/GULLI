package control.threads;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An object, that is notified when a thread has finished its job.
 *
 * @author saemann
 * @param <T>
 */
public class MultiThreadBarrier<T extends Thread> extends ThreadBarrier<T> {

    private final ArrayList<T> threads;

    public int status;

    public int finished = 0;

    public MultiThreadBarrier(String name, ThreadController controller) {
        super(name, controller);
        threads = new ArrayList<>(2);
    }

    @Override
    public void loopfinished(T finishedThread) {
//        status = 0;
        synchronized (this) {
//            status = 1;
            finished++;
            if (finished >= threads.size()) {
                //Last thread has to call the accomplished!-function
//                status = 2;
                notifyWhenReady.finishedLoop(this);
//                status = 3;
            } //else Not all are ready yet, This Thread now should fall asleep
            try {
//                status = 6;
                this.wait();
//                status = 7;
            } catch (InterruptedException ex) {
//                status = 10;
                Logger.getLogger(MultiThreadBarrier.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
//        status = 11;
    }

    public void addThread(T thread) {
        this.threads.add(thread);
    }

    @Override
    public void startover() {
        status = 100;
        synchronized (this) {
            status = 101;
            finished = 0;
            this.notifyAll();
            status = 103;
        }
    }

    @Override
    public void initialize() {
        if (isinitialized) {
            return;
        }
        isinitialized = false;
        for (Thread thread : threads) {
            try {
                thread.start();
            } catch (Exception e) {
                System.err.println("State (" + this.name + "){finished " + finished + "/" + threads.size() + "}=" + thread.getState());
            }
        }
    }

    public ArrayList<T> getThreads() {
        return this.threads;
    }

    @Override
    public void initialized(T itsMe) {
        if (isinitialized) {
            //a newly created Thread was added. it can directly proceed to the barrier
            loopfinished(itsMe);
            return;
        }

        synchronized (this) {
            //Check if this is the last Thread that finished initialization.
            for (T value : threads) {
                if (value != itsMe && value.getState() != Thread.State.WAITING) {
                    try {
                        //Not ready yet, This Thread now should fall asleep
                        this.wait();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MultiThreadBarrier.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return;
                }
            }
            isinitialized = true;
            //Notify threadcontroller : all threads on this barrier have been initialized.
            notifyWhenReady.initializingFinished(this);
            try {
                this.wait();
            } catch (InterruptedException ex) {
                Logger.getLogger(MultiThreadBarrier.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + name + "," + threads.size() + " Threads, Status=" + status + ",finished " + finished + "/" + threads.size() + "}";
    }

}
