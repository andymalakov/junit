package edu.rice.cs.cunit.tests.junit;

import junit.framework.*;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Test of test cases that do not force all threads to join.
 * @author Mathias Ricken
 */
public class NoJoinTestCaseTest extends TestCase {
    public static void main (String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    public static Test suite ( ) {
        TestSuite suite= new TestSuite("Concutest-JUnit Tests");
        suite.addTestSuite(NoJoinTestCaseTest.class);
        return suite;
    }

    // ============================================
    // Test of a test case with a "_NOJOIN" method.
    // ============================================

    public static class TestCase1 extends TestCase {
        public Thread auxThread = null;
        public TestCase1(String name) {
            super(name);
        }
        public void testShouldPass_NOJOIN() {
            auxThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(5000);
                    }
                    catch(InterruptedException e) {
                        /* ignore */
                    }
                }
            }, "Testcase1");
            auxThread.start();
            // do not join with auxThread
        }
    }
    
    /**
     * Test of a test case with a "_NOJOIN" method.
     * Note: We don't want the join tests enabled here, because this us a unit test for testing.
     *       TestCase1 passes but leaves threads alive. This test asserts that TestCase1 passes,
     *       so it should succeed if TestCase1 passes, even though it leaves threads alive.
     *       But that means we need to ignore the threads that are still alive.
     */
    public void testNoJoin_NOJOIN() {
    TestCase1 tc = new TestCase1("testShouldPass_NOJOIN");
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


    // ===============================================
    // Test of a test case without a "_NOJOIN" method.
    // ===============================================

    public static class TestCase2 extends TestCase {
        public TestCase2(String name) {
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
                }
            }, "Testcase1");
            t.start();
            // do not join with t
        }
    }
    
    /**
     * Test of a test case without a "_NOJOIN", which should fail.
     * Note: We don't want the join tests enabled here, because this us a unit test for testing.
     *       TestCase2 fails and leaves threads alive. This test asserts that TestCase2 fails,
     *       so it should succeed if TestCase2 fails. But that means we need to ignore the threads
     *       that are still alive.
     */
    public void testWithoutNoJoin_NOJOIN() {
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

    // ================================================================
    // Test of a test case with @org.junit.Test.checkJoin==false.
    // ================================================================

    public static class TestCase1Annot {
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
            }, "Testcase1Annot");
            auxThread.start();
            // do not join with auxThread
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
     * Test of a test case with @org.junit.Test.checkJoin==false.
     * Note: We don't want the join tests enabled here, because this us a unit test for testing.
     *       TestCase1Annot passes but leaves threads alive. This test asserts that TestCase1Annot passes,
     *       so it should succeed if TestCase1Annot passes, even though it leaves threads alive.
     *       But that means we need to ignore the threads that are still alive.
     */
    public void testNoJoinAnnot_NOJOIN() {
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
        try { TestCase1Annot.auxThread.join(); }
        catch(InterruptedException e) { /* ignore */ }
    }

    // ==================================================================================
    // Test of a test case with @org.junit.Test.checkJoin==true, which should fail.
    // ==================================================================================

    public static class TestCase2Annot {
        @org.junit.Test public void testShouldFail() {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(5000);
                    }
                    catch(InterruptedException e) {
                        /* ignore */
                    }
                }
            }, "Testcase1");
            t.start();
            // do not join with t
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
     * Test of a test case with @org.junit.Test.checkJoin==true, which should fail.
     * Note: We don't want the join tests enabled here, because this us a unit test for testing.
     *       TestCase2Annot fails and leaves threads alive. This test asserts that TestCase2Annot fails,
     *       so it should succeed if TestCase2Annot fails. But that means we need to ignore the threads
     *       that are still alive.
     */
    public void testWithoutNoJoinAnnot_NOJOIN() {
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
    
      
    

    // ============================================
    // Test of a test case with a "_NOJOIN" method.
    // ============================================

    public static class TestCase1AndFailure extends TestCase {
        public Thread auxThread = null;
        public TestCase1AndFailure(String name) {
            super(name);
        }
        public void testShouldPass_NOJOIN() {
            auxThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(5000);
                    }
                    catch(InterruptedException e) {
                        /* ignore */
                    }
                    fail();
                }
            }, "Testcase1");
            auxThread.start();
            // do not join with auxThread
        }
    }
    
    /**
     * Test of a test case with a "_NOJOIN" method.
     * Note: We don't want the join tests enabled here, because this us a unit test for testing.
     *       TestCase1 passes but leaves threads alive. This test asserts that TestCase1 passes,
     *       so it should succeed if TestCase1 passes, even though it leaves threads alive.
     *       But that means we need to ignore the threads that are still alive.
     */
    public void testNoJoinAndFailure_NOJOIN() {
    TestCase1AndFailure tc = new TestCase1AndFailure("testShouldPass_NOJOIN");
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
        tc.run(tr);
        if (tr.runCount()!=2) {
            fail("Test case was not executed correctly.");
        }
        // join with spawned auxiliary thread
        try { tc.auxThread.join(); }
        catch(InterruptedException e) { /* ignore */ }
    }

    // ================================================================
    // Test of a test case with @org.junit.Test.checkJoin==false.
    // ================================================================

    public static class TestCase1AndFailureAnnot {
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
                    fail();
                }
            }, "Testcase1Annot");
            auxThread.start();
            // do not join with auxThread
        }
    }

    @RunWith(Suite.class)
    @Suite.SuiteClasses({
        TestCase1AndFailureAnnot.class
        })
    public static class TestCase1AndFailureAnnotSuite {
        public static Test suite() {
            return new JUnit4TestAdapter(TestCase1AndFailureAnnotSuite.class);
        }
    }
    
    /**
     * Test of a test case with @org.junit.Test.checkJoin==false.
     * Note: We don't want the join tests enabled here, because this us a unit test for testing.
     *       TestCase1Annot passes but leaves threads alive. This test asserts that TestCase1Annot passes,
     *       so it should succeed if TestCase1Annot passes, even though it leaves threads alive.
     *       But that means we need to ignore the threads that are still alive.
     */
    public void testNoJoinAndFailureAnnot_NOJOIN() {
        Test suite = TestCase1AndFailureAnnotSuite.suite();
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
        suite.run(tr);
        if (tr.runCount()!=2) {
            fail("Test case was not executed correctly.");
        }
        // join with spawned auxiliary thread
        try { TestCase1Annot.auxThread.join(); }
        catch(InterruptedException e) { /* ignore */ }
    }

}
