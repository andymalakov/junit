package edu.rice.cs.cunit.concJUnit;

import java.lang.reflect.*;
import java.util.*;
import org.cliffc.high_scale_lib.*;

import java.awt.EventQueue;

/**
 * Test thread group.
 */
public class TestThreadGroup extends ThreadGroup {
    /**
     * Field with uncaught exception, or null if none.
     */
    private volatile Throwable _uncaughtException = null;
    
    /**
     * Field with thread that threw the uncaught exception, or null if none.
     */
    private volatile Thread _uncaughtThread = null;
    
    /**
     * Object that gets notified when the test is done, either due to success or due to an error.
     */
    private final Object _event = new Object();
    
    /**
     * True if _event has already been notified.
     */
    private volatile boolean _notified = false;
    
    /**
     * Create a new test thread group.
     */
    public TestThreadGroup() {
        super(Thread.currentThread().getThreadGroup(),
              "Concutest-JUnit-"+(int)Math.random()*10000+"-"+System.currentTimeMillis());
    }
    
    /**
     * Catch and register an uncaught exception. Also notify the event that the test is done.
     * @param t thread
     * @param e exception
     */
    public void uncaughtException(Thread t, Throwable e) {
        // System.out.println("Uncaught exception " + e.getClass().getName() + " in thread " + t +
        //                    "; current thread = " + Thread.currentThread());
        _uncaughtException = e;
        _uncaughtThread = t;
        notifyEvent();
    }
    
    /**
     * Get the uncaught exception, or null if none.
     * @return uncaught exception, or null if none
     */
    public synchronized Throwable getUncaughtException() {
        return _uncaughtException;
    }
    
    /**
     * Get the thread that threw the uncaught exception, or null if none.
     * @return thread that threw uncaught exception, or null if none
     */
    public synchronized Thread getUncaughtThread() {
        return _uncaughtThread;
    }
    
    /**
     * Get the event that gets notified when the test is done.
     * @return event that gets notified
     */
    public synchronized Object getEvent() {
        return _event;
    }
    
    /**
     * Returns true if the event has already been notified.
     * @return true if notified
     */
    public synchronized boolean hasBeenNotified() {
        return _notified;
    }
    
    
    /**
     * Sets the notified flag to true.
     */
    public synchronized void setNotified() {
        _notified = true;
    }
    
    /**
     * Wait for the event to occur, if it hasn't already occurred.
     */
    public void waitForEvent() throws InterruptedException {
        if (!hasBeenNotified()) {
            synchronized(_event) {
                if (!hasBeenNotified()) {
                    _event.wait();
                }
            }
        }
    }
    
    /**
     * Signal that the event has occurred.
     */
    public void notifyEvent() {
        setNotified();          
        synchronized(_event) {
            _event.notify();
        }
    }
    
    /**
     * Throw an uncaught exception that occurred in the specified thread group, if
     * there was one. Otherwise, do nothing.
     * @param tg thread group
     */
    public static void throwUncaughtException(TestThreadGroup tg) throws Throwable {
        if (tg.getUncaughtException()!=null) {
            Throwable tgException = tg.getUncaughtException(); 
            if (tgException instanceof junit.framework.TestCase.WrappedException) {
                tgException = tgException.getCause();
            }
            if (tgException!=null) {
                try {
                    extendStackTrace(tgException, tg.getUncaughtThread());
                }
                catch(LuckyWarningsDisabledException e) { /* nothing to do */ }
            }
            throw tgException;
        }
    }
    
    /**
     * Check if there are still some threads alive, i.e. ones that have not been joined yet.
     * @param tg thread group the threads are in
     * @param t main test thread, i.e. the thread that may have spawned others
     * @return list of living threads
     */
    public static Thread[] checkThreadsAlive(ThreadGroup tg, Thread t) {
        ArrayList<Thread> alive = new ArrayList<Thread>();
        int activeCount = tg.activeCount();
        Thread[] activeThreads = new Thread[Math.max(20, activeCount*2)];
        int enumeratedCount = tg.enumerate(activeThreads, false);
        for(int i=0; i<enumeratedCount; ++i) {
            Thread at = activeThreads[i];
            if ((at!=t) &&
                (at.isAlive()) && 
                (!at.isDaemon()) && 
                !shouldIgnoreThread(at)) {
                alive.add(at);
                // System.err.println("Thread "+at.getName()+" ("+at.getClass().getName()+") is still alive");
            }
        }
        return alive.toArray(new Thread[alive.size()]);
    }
    
    /**
     * Return the event thread, if it can be found, or null.
     * @return event thread or null
     */
    public static Thread getEventThreadOrNull() {
        // Find the root thread group
        ThreadGroup root = Thread.currentThread().getThreadGroup().getParent();
        while (root.getParent() != null) {
            root = root.getParent();
        }
        
        // Visit each thread group
        return getEventThreadOrNullHelper(root);
    }
    
    
    // This method recursively visits all thread groups under `group'.
    public static Thread getEventThreadOrNullHelper(ThreadGroup group) {
        // Get threads in `group'
        int numThreads = group.activeCount();
        Thread[] threads = new Thread[numThreads*2];
        numThreads = group.enumerate(threads, false);
        
        // Enumerate each thread in `group'
        for (int i=0; i<numThreads; ++i) {
            if (matchesEventThreadName(threads[i])) return threads[i];
        }
        
        // Get thread subgroups of `group'
        int numGroups = group.activeGroupCount();
        ThreadGroup[] groups = new ThreadGroup[numGroups*2];
        numGroups = group.enumerate(groups, false);
        
        // Recursively visit each subgroup
        for (int i=0; i<numGroups; ++i) {
            Thread t = getEventThreadOrNullHelper(groups[i]);
            if (t!=null) return t;
        }
        
        // not found
        return null;
    }
    
    /**
     * Return true if the name of the specified thread matches the event thread name.
     * @param at thread
     * @return true if the thread's name matches the event thread name
     */
    public static boolean matchesEventThreadName(Thread at) {
        // System.out.println("\tmatches AWT-EventQueue-? "+at.getName()+" --> "+(at.getName().startsWith("AWT-EventQueue-")));
        return at.getName().startsWith("AWT-EventQueue-");
    }
    
    /**
     * Return true if the specified thread should be ignored because it is a system
     * thread or a daemon.
     * @param at thread
     * @return true if thread should be ignored
     */
    public static boolean shouldIgnoreThread(Thread at) {
        // TODO: detection of system threads has to be improved
        if (at.isDaemon()) {
            return true;
        }
        
        if (at.getName().equals("AWT-Shutdown")) {
            return true;
        }
        
        if (matchesEventThreadName(at)) {
            // System.out.println("\t\t_eventThreadOrNull set! "+_eventThreadOrNull);
            return true;
        }
        
        // not necessary, is a daemon thread
        // if (at.getName().equals("AWT-Windows")) {
        //    StackTraceElement[] ste = at.getStackTrace();
        //    boolean found = false;
        //    for(int e=0; e<ste.length; ++e) {
        //        if ((ste[e].getClassName().startsWith("sun.awt."))) {
        //            found = true;
        //            break;
        //        }
        //    }
        //    if (found) {
        //        return true;
        //    }
        // }
        
        // not necessary, is a daemon thread
        // if (at.getName().equals("Java2D Disposer")) {
        //    return true;
        // }
        
        if (at.getName().startsWith("RMI ")) {
            return true;
        }
        
        ThreadGroup tg = at.getThreadGroup();
        if ((tg!=null) && (tg.getName().equals("system"))) {
            return true;
        }
        
        if (at.getName().equals("DestroyJavaVM")) {
            return true;
        }
        
        if (at.getName().equals("process reaper")) {
            return true;
        }
        
        if (at.getName().equals("Basic L&F File Loading Thread")) {
            return true;
        }
        
        if (at.getName().equals("Aqua L&F File Loading Thread")) {
            return true;
        }

        if (at.getName().matches("pool-\\d*-thread-\\d*")) {
            // execute threads in ThreadPools for now
            // we can't join them
            return true;
        }
        
        return false;
    }
    
    /**
     * Collect the thread set with the specified name starting for the given thread.
     * @param t start thread
     * @param fieldName name of the thread set
     * @return set of threads
     */
    public static HashSet<Thread> collectThreads(Thread t, String fieldName) throws LuckyWarningsDisabledException {
        try {
            HashSet<Thread> threads = new HashSet<Thread>();
            threads.add(t);
            Class cThreadSets = Class.forName("edu.rice.cs.cunit.concJUnit.ThreadSets");
            Field fThreads = cThreadSets.getField(fieldName);
            Object oThreads = fThreads.get(null);
            NonBlockingHashMap<Thread,NonBlockingHashSet<Thread>> sets = (NonBlockingHashMap<Thread,NonBlockingHashSet<Thread>>)oThreads;
            if (sets!=null) {
                NonBlockingHashSet<Thread> threadSet = sets.get(t);
                if (threadSet!=null) {
                    for(Thread ct: threadSet) {
                        threads.addAll(collectThreads(ct, fieldName));
                    }
                }
            }
            return threads;
        }
        catch(ClassNotFoundException e) {
            System.err.println("Disabled \"lucky\" warnings: "+e);
            throw new LuckyWarningsDisabledException("Disabled \"lucky\" warnings", e);
        }
        catch(NoSuchFieldException e) {
            System.err.println("Disabled \"lucky\" warnings: "+e);
            throw new LuckyWarningsDisabledException("Disabled \"lucky\" warnings", e);
        }
        catch(IllegalAccessException e) {
            System.err.println("Disabled \"lucky\" warnings: "+e);
            throw new LuckyWarningsDisabledException("Disabled \"lucky\" warnings", e);
        }
    }

    /**
     * Print thread set with the specified name.
     * @param fieldName name of the thread set
     */
    public static void dumpThreads(String fieldName) {
        System.out.println(fieldName);
        try {
            Class cThreadSets = Class.forName("edu.rice.cs.cunit.concJUnit.ThreadSets");
            Field fThreads = cThreadSets.getField(fieldName);
            Object oThreads = fThreads.get(null);
            NonBlockingHashMap<Thread,NonBlockingHashSet<Thread>> sets = (NonBlockingHashMap<Thread,NonBlockingHashSet<Thread>>)oThreads;
            System.out.println("\tsets="+sets);
            if (sets!=null) {
                for(Map.Entry<Thread,NonBlockingHashSet<Thread>> e: sets.entrySet()) {
                    System.out.println("\t"+System.identityHashCode(e.getKey())+" - "+e.getKey());
                    for(Thread t: e.getValue()) {
                        System.out.println("\t\t"+System.identityHashCode(t)+" - "+t);
                    }
                }
            }
        }
        catch(Throwable t) {
            System.out.println(t);
        }
    }

    /**
     * Return the start stack trace for the specified thread.
     * @param t thread
     * @return start stack trace
     */
    public static String getThreadStartStackTrace(Thread t) throws LuckyWarningsDisabledException {
        try {
            Class cThreadSets = Class.forName("edu.rice.cs.cunit.concJUnit.ThreadSets");
            Field fThreads = cThreadSets.getField("threadStartStackTraces");
            Object oThreads = fThreads.get(null);
            NonBlockingHashMap<Thread,String> sts = (NonBlockingHashMap<Thread,String>)oThreads;
            if (sts!=null) {
                return sts.get(t);
            }
            return null;
        }
        catch(ClassNotFoundException e) {
            throw new LuckyWarningsDisabledException("Disabled \"lucky\" warnings", e);
        }
        catch(NoSuchFieldException e) {
            throw new LuckyWarningsDisabledException("Disabled \"lucky\" warnings", e);
        }
        catch(IllegalAccessException e) {
            throw new LuckyWarningsDisabledException("Disabled \"lucky\" warnings", e);
        }
    }
    
    /**
     * Extend the stack trace in the throwable by the start stack trace of the thread.
     * @param t throwable whose stack trace should be extended
     * @param thread thread whose start stack trace should be appended to the throwable's stack trace
     */
    public static void extendStackTrace(Throwable t, Thread thread) throws LuckyWarningsDisabledException {
        String cc = getThreadStartStackTrace(thread);
        StackTraceElement[] ccStes = CreationContextError.parseCreationContextString(cc);
        StackTraceElement[] tStes = t.getStackTrace();
        StackTraceElement[] extStes = new StackTraceElement[tStes.length + ccStes.length];
        System.arraycopy(tStes, 0, extStes, 0, tStes.length);
        System.arraycopy(ccStes, 0, extStes, tStes.length, ccStes.length);
        t.setStackTrace(extStes);
    }
    
    /**
     * Exception indicating that "lucky" warnings can't be used.
     */
    public static class LuckyWarningsDisabledException extends Exception {
        /**
         * Constructor for a new exception.
         * @param message description
         * @param cause the exception that was thrown
         */
        public LuckyWarningsDisabledException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * "sun.awt.exception.handler" handler.
     */
    public static class AwtHandler {
        /**
         * Current test group for use in "sun.awt.exception.handler" handler.
         */
        private static volatile TestThreadGroup _currentThreadGroup;
        
        /**
         * Set the current thread group so the AWT handler can use it.
         * @param tg thread group
         */
        public static void setCurrentThreadGroup(TestThreadGroup tg) {
            _currentThreadGroup = tg;
        }
        
        /**
         * Get the current thread group so the AWT handler can use it.
         * @return current thread group
         */
        public static TestThreadGroup getCurrentThreadGroup() {
            return _currentThreadGroup;
        }
        
        /** Handle an uncaught exception.
          * @param t uncaught exception */
        public void handle(Throwable t) {
            // msg(this.getClass().getName()+".handle: t="+t+", tg="+_currentThreadGroup);
            if (_currentThreadGroup!=null) {
                final Thread ct = Thread.currentThread();
                _currentThreadGroup.uncaughtException(ct,t);
            }
        }
    }
    
    /**
     * A Runnable that updates the test thread group the event thread uses.
     */
    private static final Runnable UPDATE_AWT_HANDLER_THREAD_GROUP = new Runnable() {
        public void run() {
            // System.out.println("UPDATE_AWT_HANDLER_THREAD_GROUP");
            AwtHandler.setCurrentThreadGroup(TestThreadGroup.getCurrentThreadGroup());
        }
    };

    /**
     * Update the AWT handler thread group, using a Runnable if necessary.
     * If the event thread isn't running, just set the field directly. 
     */
    public static void updateAWTHandlerThreadGroup() {
        // System.out.println("updateAWTHandlerThreadGroup:");
        boolean eventThreadAlive = false;
        Thread eventThreadOrNull = getEventThreadOrNull();
        // System.out.println("eventThreadOrNull: "+eventThreadOrNull);
        if (eventThreadOrNull!=null) {
            eventThreadAlive = eventThreadOrNull.isAlive();
        }
        // System.out.println("eventThreadAlive: "+eventThreadAlive);
        if (eventThreadAlive) {
            // System.out.println("\tupdate via Runnable");
            // event thread is running, use the Runnable
            EventQueue.invokeLater(UPDATE_AWT_HANDLER_THREAD_GROUP);
        }
        else {
            // System.out.println("\tdirect update");
            // event thread not running, set directly
            AwtHandler.setCurrentThreadGroup(TestThreadGroup.getCurrentThreadGroup());
        }
    }
    
    /**
     * "sun.awt.exception.handler" dummy handler.
     */
    public static class DummyHandler {
        /** Handle an uncaught exception just by printing it.
          * @param t uncaught exception */
        public void handle(Throwable t) {
            System.err.println(t);
        }
    }
    
    /**
     * Current test group for use in "sun.awt.exception.handler" handler.
     */
    private static volatile TestThreadGroup _currentThreadGroup;

    /**
     * Set the current thread group so the AWT handler can use it.
     * @param tg thread group
     */
    public static void setCurrentThreadGroup(TestThreadGroup tg) {
        _currentThreadGroup = tg;
    }
    
    /**
     * Get the current thread group so the AWT handler can use it.
     * @return current thread group
     */
    public static TestThreadGroup getCurrentThreadGroup() {
        return _currentThreadGroup;
    }

    /**
     * Whether an EventThreadStillProcessingError already occurred. If there was such
     * an error, then we will disable the checking of EventThreadStillProcessingErrors.
     * Note that this could also indicate a deadlock in the event thread, which would
     * cause future tests to hang or fail.
     */
    private static volatile boolean _eventThreadStillProcessingErrorOccurred = false;
    
    /**
     * Set the state of the _eventThreadStillProcessingErrorOccurred flag.
     * @param b new state
     */
    public static void setEventThreadStillProcessingErrorOccurred(boolean b) {
        _eventThreadStillProcessingErrorOccurred = b;
        // System.out.println("setEventThreadStillProcessingErrorOccurred("+b+")");
        // new RuntimeException().printStackTrace(System.out);
    }
    
    /**
     * Return the state of the _eventThreadStillProcessingErrorOccurred flag.
     * @return state of _eventThreadStillProcessingErrorOccurred
     */
    public static boolean getEventThreadStillProcessingErrorOccurred() {
        return _eventThreadStillProcessingErrorOccurred;
    }
    
    
    
    /** @return false if check threads is disabled per Java property. */
    public static boolean isCheckThreadsEnabled() {
      String enabled = System.getProperty("edu.rice.cs.cunit.concJUnit.check.threads.enabled");
      return ((enabled == null) || (new Boolean(enabled)).booleanValue());
    }
    /** @return false if check join is disabled per Java property. */
    public static boolean isCheckJoinEnabled() {
      if (!isCheckThreadsEnabled()) return false;
      String enabled = System.getProperty("edu.rice.cs.cunit.concJUnit.check.join.enabled");
      return ((enabled == null) || (new Boolean(enabled)).booleanValue());
    }
    /** @return false if check lucky is disabled per Java property. */
    public static boolean isCheckLuckyEnabled() {
      if (!isCheckJoinEnabled()) return false;
      String enabled = System.getProperty("edu.rice.cs.cunit.concJUnit.check.lucky.enabled");
      return ((enabled == null) || (new Boolean(enabled)).booleanValue());
    }
    
//    public static void msg(String s) {
//        System.out.println(s);
//        try {
//            java.io.PrintWriter pw = new java.io.PrintWriter
//                (new java.io.FileWriter(new java.io.File(new java.io.File(System.getProperty("user.home")),
//                                                         "junit.txt"),
//                                        true), true);
//            pw.println(s);
//            pw.close();
//        }
//        catch(java.io.IOException ioe) { }
//    }
}
