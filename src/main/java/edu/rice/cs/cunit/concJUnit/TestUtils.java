package edu.rice.cs.cunit.concJUnit;

import java.lang.reflect.*;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.AWTEvent;

/**
 * Test utilities.
 */
public class TestUtils {
    /**
     * Wait for event thread to finish processing.
     * Note: If other threads are still running, this may be ineffective, as they may
     * continue to enqueue other Runnables.
     * @throws MultithreadedTestError if executed in event thread
     */
    public static void waitForEventThread() {
        if (EventQueue.isDispatchThread()) throw new 
            MultithreadedTestError("TestUtils.waitForEventThread() called within event thread");

        if (TestThreadGroup.getEventThreadOrNull()==null) return; // nothing to do
        
        final EventQueue q = Toolkit.getDefaultToolkit().getSystemEventQueue();
        final boolean[] finished = new boolean[] { false };
        do {
            try {
                EventQueue.invokeAndWait(new Runnable() {
                    public void run() {
                        if (q.peekEvent()==null) finished[0] = true;
                    }
                });
            }
            catch(InterruptedException ie) { /* ignore */ }
            catch(java.lang.reflect.InvocationTargetException ite) {
                throw new MultithreadedTestError("Unexpected exception", ite);
            }
        } while(!finished[0]);
    }
    
    /**
     * Wait for all other threads (except the event thread) to terminate.
     */
    public static void waitForOtherThreads() {
        ThreadGroup tg = TestThreadGroup.getCurrentThreadGroup();
        Thread ct = Thread.currentThread();
        // check which threads are still running
        final Thread[] alive = TestThreadGroup.checkThreadsAlive(tg, ct);
        // these are the threads that are not being ignored
        // this also doesn't include the current thread
        // so we can join them all with this thread
        for(Thread at: alive) {
            do {
                try {
                    // System.out.println(ct.getName()+" waiting for "+at.getName());
                    at.join();
                }
                catch(java.lang.InterruptedException ie) { /* ignore and continue */ }
            } while(at.isAlive());
        }
    }
    
    /**
     * Wait for all other threads, including the event thread if this method
     * isn't run in the event thread.
     */
    public static void waitForAll() {
        // wait for other threads first, so waitForEventThread is effective
        waitForOtherThreads();
        if (!EventQueue.isDispatchThread()) waitForEventThread(); 
    }
}
