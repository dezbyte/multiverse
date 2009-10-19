package org.multiverse.stms.alpha.manualinstrumentation;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.utils.GlobalStmInstance;
import static org.multiverse.utils.TransactionThreadLocal.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class RefTest {
    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        GlobalStmInstance.set(stm);
        setThreadLocalTransaction(null);
    }

    @After
    public void after() {
        setThreadLocalTransaction(null);
    }

    public Transaction startTransaction() {
        Transaction t = stm.startUpdateTransaction(null);
        setThreadLocalTransaction(t);
        return t;
    }

    @Test
    public void testConstruction_empty() {
        Ref ref = new Ref();
        assertTrue(ref.isNull());
    }

    @Test
    public void testConstruction_nonNull() {
        String value = "foo";
        Ref<String> ref = new Ref<String>(value);
        assertSame(value, ref.get());
    }

    @Test
    public void testConstruction_null() {
        Ref<String> ref = new Ref<String>(null);
        assertNull(ref.get());
    }

    @Test
    public void clearEmpty() {
        Ref<String> ref = new Ref<String>();
        assertNull(ref.clear());
    }

    @Test
    public void clearNonEmpty() {
        String value = "foo";
        Ref<String> ref = new Ref<String>(value);
        assertSame(value, ref.clear());
    }

    @Test
    public void setNull() {
        Ref<String> ref = new Ref<String>();
        long startVersion = stm.getClockVersion();

        ref.set(null);

        assertEquals(startVersion, stm.getClockVersion());
        assertNull(ref.get());
    }

    @Test
    public void setNotNull() {
        Ref<String> ref = new Ref<String>();
        long startVersion = stm.getClockVersion();

        String value = "foo";
        ref.set(value);

        assertEquals(startVersion + 1, stm.getClockVersion());
        assertSame(value, ref.get());
    }

    @Test
    public void testAbaProblemIsNotDetected() {
        final String a = "A";
        final String b = "B";

        final Ref<String> ref = new Ref<String>(a);

        long startVersion = stm.getClockVersion();
        Transaction t = startTransaction();
        ref.set(b);
        ref.set(a);
        t.commit();
        assertEquals(startVersion, stm.getClockVersion());
    }

}