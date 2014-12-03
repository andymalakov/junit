package edu.rice.cs.cunit.concJUnit;

import org.junit.runners.model.Statement;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.TestCase;
import java.util.concurrent.*;
import java.util.HashSet;

import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.AWTEvent;

public class RunInThreadGroup extends Statement {
    private Statement fNext;
    private Method fMethod;
    private boolean fCheckJoin = true;
    private boolean fCheckLucky = true;
    
    public RunInThreadGroup(Statement next, Method method) {
        fNext= next;
        fMethod= method;
    }
    public RunInThreadGroup(Statement next, Method method, boolean checkJoin, boolean checkLucky) {
        fNext= next;
        fMethod= method;
        fCheckJoin= checkJoin;
        fCheckLucky= checkLucky;
    }
   
    @Override
    public void evaluate() throws Throwable {
        final String prevSunHandler = System.getProperty("sun.awt.exception.handler");
        // TestThreadGroup.msg("       RunInThreadGroup.runBare: prevSunHandler="+prevSunHandler);
        try {
            final TestThreadGroup tg = new TestThreadGroup();
            TestThreadGroup.setCurrentThreadGroup(tg);
            System.setProperty("sun.awt.exception.handler", TestThreadGroup.AwtHandler.class.getName());
            TestThreadGroup.updateAWTHandlerThreadGroup();
            // TestThreadGroup.msg("-----> RunInThreadGroup.runBare: created and set tg="+TestThreadGroup.getCurrentThreadGroup());
            final Thread t = new Thread(tg, new Runnable() {
                public void run() {
                    Throwable exception= null;
                    try {
                        fNext.evaluate();
                        // fMethod.invoke(fTest);
                    }
                    catch(InvocationTargetException ite) {
                        exception = ite.getTargetException();
                    }
                    catch(Throwable running) {
                        exception = running;
                    }
                    if (exception!=null) {
//                            try {
//                                extendStackTrace(exception, Thread.currentThread());
//                            }
//                            catch(LuckyWarningsDisabledException e) { /* nothing to do */ }
                        throw new TestCase.WrappedException(exception);
                    }
                    tg.notifyEvent(); 
                }
            }, "Concutest-JUnit-"+((fMethod.getName()!=null)?fMethod.getName():"test"));
            t.setDaemon(false); // set the test thread to always be a non-daemon                
            t.start();
            try {
                tg.waitForEvent();
                try {
                    // and then join with the actual test thread
                    t.join();
                }
                catch(InterruptedException e) {
                    // there was an interrupted exception that we didn't expect
                    throw new MultithreadedTestError("After the test had finished, the join "+
                                                     "operation with the test thread was interrupted "+
                                                     "(probably a bug in Concutest-JUnit)", e);
                }
            }
            catch(InterruptedException e) {
                // this thread was interrupted, pass it on to the actual test thread
                t.interrupt();
                try {
                    // and then join with the actual test thread
                    t.join();
                }
                catch(InterruptedException e1) {
                    // there was another interrupted exception that we didn't expect
                    throw new MultithreadedTestError("After the test had been interrupted, the join "+
                                                     "operation with the test thread was interrupted "+
                                                     "(probably a bug in Concutest-JUnit)", e);
                }
            }
            // if there was an exception, wait until the throwing thread
            // is dead (but not longer than 1000 ms)
            if (tg.getUncaughtThread()!=null) {
                try {
                    tg.getUncaughtThread().join(1000);
                }
                catch(InterruptedException e) {
                    // there was another interrupted exception that we didn't expect
                    throw new MultithreadedTestError("After the test threw an exception and died, the join "+
                                                     "operation with the test thread was interrupted "+
                                                     "(probably a bug in Concutest-JUnit)", e);
                }
            }

            // check if there was an uncaught exception and throw it if there was one
            TestThreadGroup.throwUncaughtException(tg);

            if (fCheckJoin) {
                // check which threads are still running
                Thread[] alive = TestThreadGroup.checkThreadsAlive(tg, t);
                if (alive.length>0) {
                    for (Thread at: alive) { System.err.println("Thread "+at.getName()+" ("+at.getClass().getName()+") is still alive"); }
                    StringBuilder sb = new StringBuilder();
                    sb.append(fMethod.getName());
                    sb.append(": The test did not perform a join on all spawned threads.");
                    try {
                        String cc = TestThreadGroup.getThreadStartStackTrace(alive[0]);
                        throw new NoJoinError(sb.toString(),cc);
                    }
                    catch(TestThreadGroup.LuckyWarningsDisabledException e) {
                        throw new NoJoinError(sb.toString());
                    }
                    // System.err.println("WARNING: "+sb.toString());
                }
                else if (fCheckLucky) {
                    try {
                        // TestThreadGroup.dumpThreads("threadsStarted");
                        // TestThreadGroup.dumpThreads("threadsJoined");
                        HashSet<Thread> threadsStarted = TestThreadGroup.collectThreads(t,"threadsStarted");
                        HashSet<Thread> threadsJoined = TestThreadGroup.collectThreads(t,"threadsJoined");
                        // if a thread is ignored, we will treat it as another "root" of the join graph
                        // a thread has been properly joined if and only if it is reachable from the main thread or
                        // it is reachable from a thread that is ignored
                        for(Thread ct: threadsStarted) {
                            if (TestThreadGroup.shouldIgnoreThread(ct)) {
                                HashSet<Thread> threadsJoinedByIgnored = TestThreadGroup.collectThreads(ct,"threadsJoined");
                                threadsJoined.addAll(threadsJoinedByIgnored);
                            }
                        }
                        HashSet<Thread> threadsNotJoined = new HashSet<Thread>(threadsStarted);
                        threadsNotJoined.removeAll(threadsJoined);
                        HashSet<Thread> threadsInViolation = new HashSet<Thread>(threadsNotJoined);
                        for(Thread ct: threadsNotJoined) {
                            if (TestThreadGroup.shouldIgnoreThread(ct)) {
                                threadsInViolation.remove(ct);
                            }
                        }
                        if (threadsInViolation.size()>0) {
                            StringBuilder sb = new StringBuilder(fMethod.getName()+": Some spawned threads ended before the test was over, but the test did not join with them:");
                            for(Thread ct: threadsInViolation) {
                                sb.append('\n');
                                sb.append(ct);
                                sb.append(":\n");
                                sb.append(TestThreadGroup.getThreadStartStackTrace(ct));
                            }
                            throw new LuckyError(sb.toString(), TestThreadGroup.getThreadStartStackTrace(threadsInViolation.iterator().next()));
                            /*
                             System.err.println("WARNING: "+fName+": Some spawned threads ended before the test was over, but the test did not join with them:");
                             for(Thread ct: threadsInViolation) {
                             System.err.println(ct+":\n"+getThreadStartStackTrace(ct));
                             }
                             */
                        }
                    }
                    catch(TestThreadGroup.LuckyWarningsDisabledException e) {
                        System.err.println("Disabled \"lucky\" warnings: "+e);
                    }
                }
                
                // only check if this error hasn't been reported already
                if (!TestThreadGroup.getEventThreadStillProcessingErrorOccurred()) {
                    // check if the event thread was alive
                    boolean eventThreadAlive = false;
                    Thread eventThreadOrNull = TestThreadGroup.getEventThreadOrNull();
                    // System.out.println("eventThreadOrNull: "+eventThreadOrNull);
                    if (eventThreadOrNull!=null) {
                        eventThreadAlive = eventThreadOrNull.isAlive();
                    }
                    // System.out.println("eventThreadAlive: "+eventThreadAlive);
                    if (eventThreadAlive) {
                        // The event thread is alive, make sure that there are no more Runnables in it
                        // that could fail.
                        // We know that all other threads have terminated:
                        // - test's main thread finished and returned
                        // - no child threads are running (would have caused a NoJoinError otherwise)
                        // There could be a Runnable in the event thread running right now.
                        // If we add our own "token" Runnable to it and wait for it to finish,
                        // then we know the currently running Runnable has finished.
                        // However, it could still have added more Runnables to the event queue.
                        // So we check in our own Runnable if the event queue is empty.
                        final Thread eventThread = eventThreadOrNull;
                        final StackTraceElement[] eventThreadStackTrace = eventThread.getStackTrace(); 
                        final EventQueue eq = Toolkit.getDefaultToolkit().getSystemEventQueue();
                        if (eq!=null) {
                            AWTEvent ev = eq.peekEvent();
                            if (ev!=null) {
                                TestThreadGroup.setEventThreadStillProcessingErrorOccurred(true);
                                throw new EventThreadStillProcessingError("A Runnable was still running in the event "+
                                                                          "thread after the test had already ended. "+
                                                                          "(Note: This error will not "+
                                                                          "be reported anymore in the following tests.)",
                                                                          eventThreadStackTrace);
                            }
                            else {
                                // event queue is empty
                                // add our own Runnable
                                Thread interrupterThread = null;
                                try {
                                    // interrupt the invokeAndWait below after 100 ms
                                    final Object signal = new Object();
                                    final boolean[] tokenRunning = new boolean[] { false };
                                    final boolean[] timedOut = new boolean[] { false };
                                    
                                    final Thread mainThread = Thread.currentThread();
                                    interrupterThread = new Thread("Concutest-JUnit-EventThreadWaitInterrupter") {
                                        public void run() {
                                            // wait for at most 100 ms, then interrupt mainThread
                                            synchronized(signal) {
                                                try { signal.wait(100); }
                                                catch(InterruptedException e) {
                                                    MultithreadedTestError mte =
                                                        new MultithreadedTestError("After the test's main thread finished and the "+
                                                                                   "test framework waited for the event thread to "+
                                                                                   "finish processing, a wait operation was "+
                                                                                   "interrupted unexpectedly "+
                                                                                   "(probably a bug in Concutest-JUnit)", e);
                                                    tg.uncaughtException(Thread.currentThread(), mte);
                                                }
                                            }
                                            synchronized(signal) {
                                                if (!tokenRunning[0]) {
                                                    // if our token isn't running at this point,
                                                    // interrupt the main thread
                                                    timedOut[0] = true;
                                                    mainThread.interrupt();
                                                }
                                            }
                                        }
                                    };
                                    interrupterThread.start();
                                    
                                    eq.invokeAndWait(new Runnable() {
                                        public void run() {
                                            // signal the interrupterThread it doesn't have to wait any longer 
                                            synchronized(signal) {
                                                tokenRunning[0] = true;
                                                signal.notify();
                                            }
                                            
                                            // check if event queue is empty
                                            AWTEvent ev = eq.peekEvent();
                                            if (ev!=null) {
                                                // not empty! that means a Runnable executing after the thread
                                                // finished added another Runnable to the event queue
                                                synchronized(signal) {
                                                    if (!timedOut[0]) {
                                                        TestThreadGroup.setEventThreadStillProcessingErrorOccurred(true);
                                                        EventThreadStillProcessingError ete =
                                                            new EventThreadStillProcessingError("A Runnable was still running in the event "+
                                                                                                "thread after the test had already ended, "+
                                                                                                "and a new Runnable was even added to the "+
                                                                                                "event queue. (Note: This error will not "+
                                                                                                "be reported anymore in the following tests.)",
                                                                                                eventThreadStackTrace);
                                                        tg.uncaughtException(Thread.currentThread(), ete);
                                                    }
                                                }
                                            }
                                        }
                                    });
                                }
                                catch(InterruptedException ie) {
                                    TestThreadGroup.setEventThreadStillProcessingErrorOccurred(true);
                                    throw new EventThreadStillProcessingError("A Runnable was still running in the event "+
                                                                              "thread after the test had already ended. "+
                                                                              "(Note: This error will not "+
                                                                              "be reported anymore in the following tests.)",
                                                                              eventThreadStackTrace);
                                }
                                catch(InvocationTargetException ite) {
                                    throw new MultithreadedTestError("After the test's main thread finished and the "+
                                                                     "test framework waited for the event thread to "+
                                                                     "finish processing, the framework's own token "+
                                                                     "Runnable unexpectedly threw an exception "+
                                                                     "(probably a bug in Concutest-JUnit)", ite);
                                }
                                finally {
                                    try { interrupterThread.join(); }
                                    catch(InterruptedException ie) { /* ignore */ }
                                }
                                
                                // check if there was an uncaught exception and throw it if there was one
                                TestThreadGroup.throwUncaughtException(tg);
                            }
                        }
                    }
                }
            }
        }
        finally {
            if (prevSunHandler!=null) {
                System.setProperty("sun.awt.exception.handler", prevSunHandler);
                // TestThreadGroup.msg("<----- RunInThreadGroup.runBare: reset prevSunHandler="+prevSunHandler);
            }
            else {
                // can't be set back to null, so we install a dummy handler
                System.setProperty("sun.awt.exception.handler", TestThreadGroup.DummyHandler.class.getName());
                // TestThreadGroup.msg("<~~~~~ RunInThreadGroup.runBare: reset to "+TestThreadGroup.DummyHandler.class.getName());
            }
        }
    }
}