package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.TestUtils.assertIsActive;
import org.multiverse.api.annotations.AtomicMethod;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.utils.GlobalStmInstance;
import static org.multiverse.utils.TransactionThreadLocal.getThreadLocalTransaction;
import org.multiverse.datastructures.refs.IntRef;

public class AtomicMethod_StaticMethodTest {
    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        GlobalStmInstance.set(stm);
    }

    @After
    public void tearDown() {
        //assertNoInstrumentationProblems();
    }

    public static void assertTransactionWorking() {
        assertIsActive(getThreadLocalTransaction());
    }

    @Test
    public void testSimpleStaticMethod() {
        long version = stm.getClockVersion();

        StaticNoArgMethod.doIt();

        assertEquals(version, stm.getClockVersion());
    }

    public static class StaticNoArgMethod {

        @AtomicMethod
        static void doIt() {
            assertTransactionWorking();
        }
    }

    @Test
    public void testComplexStaticMethod() {
        StaticComplexMethod.aExpected = 10;
        StaticComplexMethod.bExpected = 1000L;
        StaticComplexMethod.cExpected = "";
        StaticComplexMethod.result = 400;

        int result = StaticComplexMethod.doIt(
                StaticComplexMethod.aExpected,
                StaticComplexMethod.bExpected,
                StaticComplexMethod.cExpected);
        assertEquals(StaticComplexMethod.result, result);
    }

    public static class StaticComplexMethod {
        static int aExpected;
        static long bExpected;
        static String cExpected;
        static int result;

        @AtomicMethod
        static int doIt(int a, long b, String c) {
            assertTransactionWorking();
            assertEquals(aExpected, a);
            assertEquals(bExpected, b);
            assertSame(cExpected, c);
            return result;
        }
    }

    @Test
    public void atomicObjectsArePassedToStaticMethod() {
        IntRef a = new IntRef(10);
        IntRef b = new IntRef(20);

        long version = stm.getClockVersion();
        swap(a, b);
        assertEquals(version + 1, stm.getClockVersion());
        assertEquals(20, a.get());
        assertEquals(10, b.get());
    }

    @AtomicMethod
    public static void swap(IntRef a, IntRef b) {
        int oldA = a.get();
        a.setValue(b.get());
        b.setValue(oldA);
    }
}