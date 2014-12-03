package edu.rice.cs.cunit.tests.junit;

import junit.framework.*;
import org.junit.runners.Suite;
import org.junit.runner.RunWith;

/**
 * Test case for multithreaded tests.
 * @author Mathias Ricken
 */
public class MultithreadedTestCaseTest extends TestCase {
    public static void main (String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    public static Test suite ( ) {
        TestSuite suite= new TestSuite("Concutest-JUnit Tests");
        suite.addTestSuite(MultithreadedTestCaseTest.class);
        return suite;
    }

    // ========================================
    // Failure in a thread spawned by the test.
    // ========================================
    
    public static class TestCase1 extends TestCase {
        public TestCase1(String name) {
            super(name);
        }
        public void testShouldFail() {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(5000);
                    }
                    catch(InterruptedException e) {
                        /* ignore */
                    }
                    fail("This test case should fail!");
                }
            }, "Testcase1");
            t.start();
            try { t.join(); } catch(InterruptedException e) { }
        }
    }

    /**
     * Test of a failure in another thread.
     */
    public void testFailureInOtherThread() {
        TestCase tc = new TestCase1("testShouldFail");
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
                if (_failureT == null) {
                    if (_errorT != null) {
                        fail("Test case reported an error instead of a failure.");
                    }
                    else {
                        fail("Test case did not report a failure.");
                    }
                }
                else {
                    if (_errorT != null) {
                        fail("Test case also reported an error.");
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

    // ============================================================
    // Failure in a thread spawned by a thread spawned by the test.
    // ============================================================

    public static class TestCase2 extends TestCase {
        public TestCase2(String name) {
            super(name);
        }
        public void testShouldFail2() {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    Thread t2 = new Thread(new Runnable() {
                        public void run() {
                            try {
                                Thread.sleep(5000);
                            }
                            catch(InterruptedException e) {
                                /* ignore */
                            }
                            fail("This test case should fail!");
                        }
                    }, "Testcase2-1");
                    t2.start();
                    try { t2.join(); } catch(InterruptedException e) { /* ignored */ }
                }
            }, "Testcase2");
            t.start();
            try { t.join(); } catch(InterruptedException e) { /* ignored */ }
        }
    }

    /**
     * Test of a failure in a thread started by another thread.
     */
    public void testFailureInThreadOfAnotherThread() {
        TestCase tc = new TestCase2("testShouldFail2");
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
                if (_failureT == null) {
                    if (_errorT != null) {
                        fail("Test case reported an error instead of a failure.");
                    }
                    else {
                        fail("Test case did not report a failure.");
                    }
                }
                else {
                    if (_errorT != null) {
                        fail("Test case also reported an error.");
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

    // =====================================================
    // Forgetting to join with a thread spawned by the test.
    // =====================================================

    public static class TestCase3 extends TestCase {
        public TestCase3(String name) {
            super(name);
        }
        public void testShouldFail3() {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try { java.lang.Thread.sleep(10000); } catch(InterruptedException e) { /* ignored */ }
                }
            }, "TestCase3");
            t.start();
        }
    }
    
    /* Note: We don't want the join tests enabled here, because this us a unit test for testing.
     *       TestCase3 fails and leaves threads alive. This test asserts that TestCase3 fails,
     *       so it should succeed if TestCase3 fails. But that means we need to ignore the threads
     *       that are still alive.
     */
    public void testForgetJoin_NOJOIN() {
        TestCase tc = new TestCase3("testShouldFail3");
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
                    if (_failureT != null) {
                        fail("Test case reported a failure instead of an error.");
                    }
                    else {
                        fail("Test case did not report an error.");
                    }
                }
                else {
                    if (_failureT != null) {
                        fail("Test case also reported a failure.");
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

    // =====================================================
    // Failure in a thread spawned by the test using JUnit4.
    // =====================================================

    public static class TestCase1Annot {
        @org.junit.Test public void shouldFailAnnot() {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(5000);
                    }
                    catch(InterruptedException e) {
                        /* ignore */
                    }
                    fail("This test case should fail!");
                }
            }, "Testcase1Annot");
            t.start();
            try { t.join(); } catch(InterruptedException e) { }
        }
    }

    @RunWith(Suite.class)
    @Suite.SuiteClasses({
        TestCase1Annot.class
        })
    public static class TestCase1AnnotSuite {
        public static Test suite() {
            return new JUnit4TestAdapter(TestCase1AnnotSuite.class);
        }
    }

    /**
     * Test of a failure in another thread.
     */
    public void testFailureInOtherThreadAnnot() {
        Test suite = TestCase1AnnotSuite.suite();
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
                    if (_failureT != null) {
                        fail("Test case reported a failure instead of an error (in JUnit4 tests, only errors are used).");
                    }
                    else {
                        fail("Test case did not report an error.");
                    }
                }
                else {
                    if (_failureT != null) {
                        fail("Test case also reported a failure (in JUnit4 tests, only errors are used).");
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
    }

    // =========================================================================
    // Failure in a thread spawned by a thread spawned by the test using JUnit4.
    // =========================================================================

    public static class TestCase2Annot {
        @org.junit.Test public void testShouldFail2Annot() {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    Thread t2 = new Thread(new Runnable() {
                        public void run() {
                            try {
                                Thread.sleep(5000);
                            }
                            catch(InterruptedException e) {
                                /* ignore */
                            }
                            fail("This test case should fail!");
                        }
                    }, "Testcase2Annot-1");
                    t2.start();
                    try { t2.join(); } catch(InterruptedException e) { /* ignored */ }
                }
            }, "Testcase2Annot");
            t.start();
            try { t.join(); } catch(InterruptedException e) { /* ignored */ }
        }
    }

    @RunWith(Suite.class)
    @Suite.SuiteClasses({
        TestCase2Annot.class
        })
    public static class TestCase2AnnotSuite {
        public static Test suite() {
            return new JUnit4TestAdapter(TestCase2AnnotSuite.class);
        }
    }
    
    /**
     * Test of a failure in a thread started by another thread.
     * Note: We don't want the join tests enabled here, because this us a unit test for testing.
     *       TestCase2Annot fails and leaves threads alive. This test asserts that TestCase2Annot fails,
     *       so it should succeed if TestCase1Annot fails. But that means we need to ignore the threads
     *       that are still alive.
     */
    public void testFailureInThreadOfAnotherThreadAnnot_NOJOIN() {
        Test suite = TestCase2AnnotSuite.suite();
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
                    if (_failureT != null) {
                        fail("Test case reported a failure instead of an error (in JUnit4 tests, only errors are used).");
                    }
                    else {
                        fail("Test case did not report an error.");
                    }
                }
                else {
                    if (_failureT != null) {
                        fail("Test case also reported a failure (in JUnit4 tests, only errors are used).");
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
    }

    // ==================================================================
    // Forgetting to join with a thread spawned by the test using JUnit4.
    // ==================================================================

    public static class TestCase3Annot {
        @org.junit.Test public void testShouldFail3Annot() {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try { java.lang.Thread.sleep(10000); } catch(InterruptedException e) { /* ignored */ }
                }
            }, "TestCase3");
            t.start();
        }
    }

    @RunWith(Suite.class)
    @Suite.SuiteClasses({
        TestCase3Annot.class
        })
    public static class TestCase3AnnotSuite {
        public static Test suite() {
            return new JUnit4TestAdapter(TestCase3AnnotSuite.class);
        }
    }
    
    /**
     * Test of forgetting to join.
     * Note: We don't want the join tests enabled here, because this us a unit test for testing.
     *       TestCase3Annot fails and leaves threads alive. This test asserts that TestCase3Annot fails,
     *       so it should succeed if TestCase1Annot fails. But that means we need to ignore the threads
     *       that are still alive.
     */
    public void testForgetJoinAnnot_NOJOIN() {
        Test suite = TestCase3AnnotSuite.suite();
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
                    if (_failureT != null) {
                        fail("Test case reported a failure instead of an error (in JUnit4 tests, only errors are used).");
                    }
                    else {
                        fail("Test case did not report an error.");
                    }
                }
                else {
                    if (_failureT != null) {
                        fail("Test case also reported a failure (in JUnit4 tests, only errors are used).");
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
    }
    
    // ======================================
    // Failure in the AWT/Swing event thread.
    // ======================================
    
    public static class TestCase4 extends TestCase {
        public TestCase4(String name) {
            super(name);
        }
        public void testShouldFail() {
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    throw new RuntimeException("Boo!");
                }
            });
            try {
                Thread.sleep(1000);
            }
            catch(InterruptedException ie) { }
        }
    }

    /**
     * Test of a failure in another thread.
     * Note: We don't want the join tests enabled here, because this us a unit test for testing.
     *       TestCase4 fails and leaves threads alive. This test asserts that TestCase4 fails,
     *       so it should succeed if TestCase4 fails. But that means we need to ignore the threads
     *       that are still alive.
     */
    public void testFailureInEventThread_NOJOIN() {
        TestCase tc = new TestCase4("testShouldFail");
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
                    if (_failureT != null) {
                        fail("Test case reported a failure instead of an error.");
                    }
                    else {
                        fail("Test case did not report an error.");
                    }
                }
                else {
                    if (_failureT != null) {
                        fail("Test case also reported a failure.");
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
}
