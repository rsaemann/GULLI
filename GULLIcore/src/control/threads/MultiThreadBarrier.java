package control.threads;

import java.util.ArrayList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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

    private final Lock lock = new ReentrantLock();
    private final String waitObject = new String();

    private int numberOfThreads=0;
//    private CyclicBarrier cyclicbarrier;
//    private int maxIndexofParticlethreads = 0;

    public MultiThreadBarrier(String name, ThreadController controller) {
        super(name, controller);
        threads = new ArrayList<>(2);
//        cyclicbarrier = new CyclicBarrier(controller.getParticleThreads().length, this);
    }

    @Override
    public void loopfinished(T finishedThread) {

//        try {
//            if(cyclicbarrier.getNumberWaiting()==maxIndexofParticlethreads){
//                notifyWhenReady.finishedLoop(this);
//            }
//            int index=cyclicbarrier.await();
//            System.out.println(" stop as thread nr "+index+": "+finishedThread.getName());
//            if(cyclicbarrier.await()==maxIndexofParticlethreads){
////                
//            }
//            System.out.println("loop :"+notifyWhenReady.getSteps()+"  "+finishedThread.getName());
        try {
            lock.lock();
            finished++;
            if (finished >= numberOfThreads) {
                //Last thread has to call the accomplished!-function
                notifyWhenReady.finishedLoop(this);
            } //else Not all are ready yet, This Thread now should fall asleep

        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            
        }
        synchronized (waitObject) {
            lock.unlock();
            try {
                waitObject.wait();
            } catch (InterruptedException ex) {
                Logger.getLogger(MultiThreadBarrier.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

////        status = 11;
//        } catch (InterruptedException ex) {
//            Logger.getLogger(MultiThreadBarrier.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (BrokenBarrierException ex) {
//            Logger.getLogger(MultiThreadBarrier.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }

    public void addThread(T thread) {
        this.threads.add(thread);
        numberOfThreads=threads.size();
//        maxIndexofParticlethreads = threads.size() - 1;
//        cyclicbarrier = new CyclicBarrier(this.threads.size() + 1, this);
    }

    @Override
    public void startover() {

//        status = 100;
//        try {
//            System.out.println("start as thread nr "+cyclicbarrier.await());
        lock.lock();
        try {
            synchronized (waitObject) {
//                status = 101;
                finished = 0;
//                status = 102;
                waitObject.notifyAll();
//                status = 103;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
//        } catch (InterruptedException ex) {
//            Logger.getLogger(MultiThreadBarrier.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (BrokenBarrierException ex) {
//            Logger.getLogger(MultiThreadBarrier.class.getName()).log(Level.SEVERE, null, ex);
//        }

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
//        try {
            if (isinitialized) {
                //a newly created Thread was added. it can directly proceed to the barrier
                loopfinished(itsMe);
                return;
            }
//            System.out.println(" initstop as thread nr "+cyclicbarrier.await()+": "+itsMe.getName());
//            lock.lockInterruptibly();
            try {
                synchronized (waitObject) {
                    //Check if this is the last Thread that finished initialization.
                    for (T value : threads) {
                        if (value != itsMe && value.getState() != Thread.State.WAITING) {
                            try {
                                //Not ready yet, This Thread now should fall asleep
                                waitObject.wait();
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
                        waitObject.wait();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MultiThreadBarrier.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
//                lock.unlock();
            }
//        } catch (InterruptedException ex) {
//            Logger.getLogger(MultiThreadBarrier.class.getName()).log(Level.SEVERE, null, ex);
//        } 
//        catch (BrokenBarrierException ex) {
//            Logger.getLogger(MultiThreadBarrier.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + name + "," + threads.size() + " Threads, Status=" + status + ",finished " + finished + "/" + threads.size() + ", locked:" + ((ReentrantLock) lock).isLocked() + "}";
    }

}
