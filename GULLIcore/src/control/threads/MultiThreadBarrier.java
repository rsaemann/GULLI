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
        synchronized (this) {
            finished++;
            if (finished >= threads.size()) {
                //Last thread has to call the accomplished!-function
                notifyWhenReady.finishedLoop(this);
            } //else Not all are ready yet, This Thread now should fall asleep
            try {
                this.wait();
            } catch (InterruptedException ex) {
                Logger.getLogger(MultiThreadBarrier.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void addThread(T thread) {
        this.threads.add(thread);
    }

    @Override
    public void startover() {
        synchronized (this) {
            finished = 0;
            this.notifyAll();
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
