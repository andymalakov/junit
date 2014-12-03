package edu.rice.cs.cunit.tests.junit;

import junit.framework.*;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import javax.swing.SwingUtilities;

import java.util.concurrent.CountDownLatch;

/**
 * Test the TestUtils.wait* methods.
 * @author Mathias Ricken
 */
public class TestUtilsWaitTest extends TestCase {
    public static void main (String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    public static Test suite ( ) {
        TestSuite suite= new TestSuite("Concutest-JUnit Tests");
        suite.addTestSuite(TestUtilsWaitTest.class);
        return suite;
    }
    
    // ===============================================
    // Test of a test case that should pass.
    // ===============================================
    
    public static class TestCase0 extends TestCase {
        public TestCase0(String name) {
            super(name);
        }
        public void testShouldPass() {
            edu.rice.cs.cunit.concJUnit.TestUtils.waitForAll();
        }
    }
    
    /**
     * Test of a test case which should pass.
     */
    public void testMainThread() {
        // System.out.println("testMainThread");
        TestCase tc = new TestCase0("testShouldPass");
        TestResult tr = new TestResult();
        tr.addListener(new TestListener() {
            private Throwable _errorT = null;
            private Throwable _failureT = null;
            public void addError(Test test, java.lang.Throwable t) {
                _errorT = t;
            }
            public void addFailure(Test test, AssertionFailedError t) {
                _failureT = t;
            }
            public void endTest(Test test) {
                if (_failureT != null) {
                    if (_errorT != null) {
                        fail("Test case reported an error and a failure.");
                    }
                    else {
                        fail("Test case reported a failure.");
                    }
                }
                else {
                    if (_errorT != null) {
                        fail("Test case reported an error.");
                    }
                }
            }
            public void startTest(Test test) {
            }
        });
        tc.run(tr);
        if (tr.runCount()!=1) {
            fail("Test case was not executed correctly.");
        }
    }
    
    // ===============================================
    // Test of a test case that should pass.
    // ===============================================
    
    public static class TestCase1 extends TestCase {
        public TestCase1(String name) {
            super(name);
        }
        public void testShouldPass() throws InterruptedException {
            // we run something in the event thread, but wait longer
            final CountDownLatch signal = new CountDownLatch(1);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    signal.countDown();
                }
            });
            signal.await();
            edu.rice.cs.cunit.concJUnit.TestUtils.waitForAll();
        }
    }
    
    /**
     * Test of a test case which should pass.
     */
    public void testEventThread() {
        // System.out.println("testEventThread");
        TestCase tc = new TestCase1("testShouldPass");
        TestResult tr = new TestResult();
        tr.addListener(new TestListener() {
            private Throwable _errorT = null;
            private Throwable _failureT = null;
            public void addError(Test test, java.lang.Throwable t) {
                _errorT = t;
            }
            public void addFailure(Test test, AssertionFailedError t) {
                _failureT = t;
            }
            public void endTest(Test test) {
                if (_failureT != null) {
                    if (_errorT != null) {
                        fail("Test case reported an error and a failure.");
                    }
                    else {
                        fail("Test case reported a failure.");
                    }
                }
                else {
                    if (_errorT != null) {
                        fail("Test case reported an error.");
                    }
                }
            }
            public void startTest(Test test) {
            }
        });
        tc.run(tr);
        if (tr.runCount()!=1) {
            fail("Test case was not executed correctly.");
        }
    }
    
    // ===============================================
    // Test of a test case with a late failure in the
    // event thread.
    // ===============================================
    
    public static class TestCase2 extends TestCase {
        public TestCase2(String name) {
            super(name);
        }
        public void testShouldFail() {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(5000);
                    }
                    catch(InterruptedException e) {
                        /* ignore */
                    }
                    throw new RuntimeException("booh!");
                }
            });
            edu.rice.cs.cunit.concJUnit.TestUtils.waitForAll();
        }
    }
    
    /**
     * Test of a test case with a late failure in the event thread.
     */
    public void testEventThreadLate() {
        // System.out.println("testEventThreadLate");
        TestCase tc = new TestCase2("testShouldFail");
        TestResult tr = new TestResult();
        tr.addListener(new TestListener() {
            private Throwable _errorT = null;
            private Throwable _failureT = null;
            public void addError(Test test, java.lang.Throwable t) {
                _errorT = t;
            }
            public void addFailure(Test test, AssertionFailedError t) {
                _failureT = t;
            }
            public void endTest(Test test) {
                if (_errorT == null) {
                    if (_failureT == null) {
                        fail("Test case did not report an error.");
                    }
                    else {
                        fail("Test case reported a failure instead of an error.");
                    }
                }
                else {
                    if (_failureT != null) {
                        fail("Test case reported an error and a failure.");
                    }
                }
            }
            public void startTest(Test test) {
            }
        });
        tc.run(tr);
        if (tr.runCount()!=1) {
            fail("Test case was not executed correctly.");
        }
    }
    
    // ===============================================
    // Test of a test case in which the event thread
    // is still processing late, but does not fail.
    // ===============================================
    
    public static class TestCase3 extends TestCase {
        public TestCase3(String name) {
            super(name);
        }
        public void testShouldPass() {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(5000);
                    }
                    catch(InterruptedException e) {
                        /* ignore */
                    }
                }
            });
            edu.rice.cs.cunit.concJUnit.TestUtils.waitForAll();
        }
    }
    
    /**
     * Test of a test case with a late failure in the event thread, which should pass.
     */
    public void testEventThreadJustLateNoFail() {
        // System.out.println("testEventThreadJustLateNoFail_NOJOIN");
        TestCase tc = new TestCase3("testShouldPass");
        TestResult tr = new TestResult();
        tr.addListener(new TestListener() {
            private Throwable _errorT = null;
            private Throwable _failureT = null;
            public void addError(Test test, java.lang.Throwable t) {
                _errorT = t;
            }
            public void addFailure(Test test, AssertionFailedError t) {
                _failureT = t;
            }
            public void endTest(Test test) {
                if (_failureT != null) {
                    if (_errorT != null) {
                        fail("Test case reported an error and a failure.");
                    }
                    else {
                        fail("Test case reported a failure.");
                    }
                }
                else {
                    if (_errorT != null) {
                        fail("Test case reported an error.");
                    }
                }
            }
            public void startTest(Test test) {
            }
        });
        tc.run(tr);
        if (tr.runCount()!=1) {
            fail("Test case was not executed correctly.");
        }
    }
    
    // ===============================================
    // Test of a test case in which the event thread
    // is processing late, and where new Runnables
    // get added after the token Runnable.
    // ===============================================
    
    public static class TestCase4 extends TestCase {
        public TestCase4(String name) {
            super(name);
        }
        public void testShouldPass() throws InterruptedException {
            final CountDownLatch signal = new CountDownLatch(1);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    signal.countDown();
                    SwingUtilities.invokeLater(new RunnableStarterRunnable(10));
                }
            });
            signal.await();
            edu.rice.cs.cunit.concJUnit.TestUtils.waitForAll();
        }
        
        public static class RunnableStarterRunnable implements Runnable {
            private final int _num;
            public RunnableStarterRunnable(int num) {
                _num = num;
            }
            public void run() {
                try {
                    Thread.sleep(50);
                }
                catch(InterruptedException e) {
                    /* ignore */
                }
                if (_num>0) {
                    SwingUtilities.invokeLater(new RunnableStarterRunnable(_num-1));
                }
            }
        }
    }
    
    /**
     * Test of a test case with a late failure in the event thread, which should pass.
     */
    public void testEventThreadLateNoFailRunnablesAfterToken() {
        // System.out.println("testEventThreadLateNoFailRunnablesAfterToken_NOJOIN");
        TestCase tc = new TestCase4("testShouldPass");
        TestResult tr = new TestResult();
        tr.addListener(new TestListener() {
            private Throwable _errorT = null;
            private Throwable _failureT = null;
            public void addError(Test test, java.lang.Throwable t) {
                _errorT = t;
            }
            public void addFailure(Test test, AssertionFailedError t) {
                _failureT = t;
            }
            public void endTest(Test test) {
                if (_failureT != null) {
                    if (_errorT != null) {
                        fail("Test case reported an error and a failure.");
                    }
                    else {
                        fail("Test case reported a failure.");
                    }
                }
                else {
                    if (_errorT != null) {
                        fail("Test case reported an error.");
                    }
                }
            }
            public void startTest(Test test) {
            }
        });
        tc.run(tr);
        if (tr.runCount()!=1) {
            fail("Test case was not executed correctly.");
        }
    }
    
    public static class TestCase5 extends TestCase {
        public Thread auxThread = null;
        public TestCase5(String name) {
            super(name);
        }
        public void testShouldPass() {
            auxThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(5000);
                    }
                    catch(InterruptedException e) {
                        /* ignore */
                    }
                }
            }, "TestCase5");
            auxThread.start();
            edu.rice.cs.cunit.concJUnit.TestUtils.waitForAll();
        }
    }
    
    public void testNoJoin() {
        TestCase5 tc = new TestCase5("testShouldPass");
        TestResult tr = new TestResult();
        tr.addListener(new TestListener() {
            private Throwable _errorT = null;
            private Throwable _failureT = null;
            public void addError(Test test, java.lang.Throwable t) {
                _errorT = t;
            }
            public void addFailure(Test test, AssertionFailedError t) {
                _failureT = t;
            }
            public void endTest(Test test) {
                if (_failureT != null) {
                    if (_errorT != null) {
                        fail("Test case reported an error and a failure.");
                    }
                    else {
                        fail("Test case reported a failure.");
                    }
                }
                else {
                    if (_errorT != null) {
                        fail("Test case reported an error.");
                    }
                }
            }
            public void startTest(Test test) {
            }
        });
        tc.run(tr);
        if (tr.runCount()!=1) {
            fail("Test case was not executed correctly.");
        }
        // join with spawned auxiliary thread
        try { tc.auxThread.join(); }
        catch(InterruptedException e) { /* ignore */ }
    }

    public static class TestCase5Annot {
        public static Thread auxThread = null;
        @org.junit.Test(checkJoin=false) public void testShouldPass() {
            auxThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(5000);
                    }
                    catch(InterruptedException e) {
                        /* ignore */
                    }
                }
            }, "TestCase5Annot");
            auxThread.start();
            edu.rice.cs.cunit.concJUnit.TestUtils.waitForAll();
        }
    }

    @RunWith(Suite.class)
    @Suite.SuiteClasses({TestCase5Annot.class})
    public static class TestCase5AnnotSuite {
        public static Test suite() {
            return new JUnit4TestAdapter(TestCase5AnnotSuite.class);
        }
    }
    
    public void testNoJoinAnnot() {
        Test suite = TestCase5AnnotSuite.suite();
        TestResult tr = new TestResult();
        tr.addListener(new TestListener() {
            private Throwable _errorT = null;
            private Throwable _failureT = null;
            public void addError(Test test, java.lang.Throwable t) {
                _errorT = t;
            }
            public void addFailure(Test test, AssertionFailedError t) {
                _failureT = t;
            }
            public void endTest(Test test) {
                if (_errorT != null) {
                    if (_failureT != null) {
                        fail("Test case reported an error and a failure (in JUnit4 tests, only errors are used).");
                    }
                    else {
                        fail("Test case reported an error.");
                    }
                }
                else {
                    if (_failureT != null) {
                        fail("Test case reported a failure (in JUnit4 tests, only errors are used).");
                    }
                }
            }
            public void startTest(Test test) {
            }
        });
        suite.run(tr);
        if (tr.runCount()!=1) {
            fail("Test case was not executed correctly.");
        }
        // join with spawned auxiliary thread
        try { TestCase5Annot.auxThread.join(); }
        catch(InterruptedException e) { /* ignore */ }
    }
}
