package control.threads;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An object, that is notified when a thread has finished its job.
 *
 * @author saemann
 * @param <T>
 */
public class SingleThreadBarrier<T extends Thread> extends ThreadBarrier<T> {

    private T thread;

    public SingleThreadBarrier(String name, ThreadController controller) {
        super(name, controller);
    }

    public SingleThreadBarrier(String name, ThreadController controller, T thread) {
        this(name, controller);
        this.thread = thread;
    }

    public void setThread(T thread) {
        this.thread = thread;
    }

    public T getThread() {
        return thread;
    }

    @Override
    public void loopfinished(T callingThread) {
        synchronized (this) {
            //  System.out.print("     " + itsMe + "finished ");
            if (callingThread == thread) {
                notifyWhenReady.finishedLoop(this);
            }
            try {
                this.wait();
            } catch (InterruptedException ex) {
                Logger.getLogger(SingleThreadBarrier.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void initialize() {
        if (isinitialized) {
            return;
        }
        isinitialized = false;
        try {
            thread.start();
        } catch (Exception e) {
            System.err.println("State (" + this.name + ")=" + thread.getState());
        }

    }

    @Override
    public void initialized(T itsMe) {
        synchronized (this) {
            if (itsMe != thread) {
                return;
            }
            isinitialized = true;
            notifyWhenReady.initializingFinished(this);
            try {
                this.wait();
            } catch (InterruptedException ex) {
                Logger.getLogger(SingleThreadBarrier.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
