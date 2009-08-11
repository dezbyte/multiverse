package org.multiverse.stms.alpha.writeset;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.DummyTransaction;
import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.manualinstrumentation.IntRef;
import org.multiverse.stms.alpha.manualinstrumentation.IntRefTranlocal;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.utils.GlobalStmInstance;
import static org.multiverse.utils.TransactionThreadLocal.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class FailFastWriteSetLockPolicyTest {
    private AlphaStm stm;
    private FailFastWriteSetLockPolicy policy;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        GlobalStmInstance.set(stm);
        setThreadLocalTransaction(null);
        policy = new FailFastWriteSetLockPolicy();
    }

    @Test
    public void nullWriteSetShouldSucceed() {
        WriteSet writeSet = null;
        boolean result = policy.acquireLocks(writeSet, new DummyTransaction());
        assertTrue(result);
    }

    @Test
    public void singletonWriteSetWithFreeLock() {
        Transaction t = new DummyTransaction();

        IntRef intValue = IntRef.createUncommitted();
        IntRefTranlocal tranlocalIntValue = new IntRefTranlocal(intValue, 0);

        WriteSet writeSet = new WriteSet(null, tranlocalIntValue);

        boolean result = policy.acquireLocks(writeSet, t);
        assertTrue(result);
        assertSame(t, intValue.getLockOwner());
    }

    @Test
    public void nonSingletonWriteSetWithFreeLock() {
        Transaction t = new DummyTransaction();

        IntRef intValue1 = IntRef.createUncommitted();
        IntRefTranlocal tranlocalIntValue1 = new IntRefTranlocal(intValue1, 0);

        IntRef intValue2 = IntRef.createUncommitted();
        IntRefTranlocal tranlocalIntValue2 = new IntRefTranlocal(intValue2, 0);

        IntRef intValue3 = IntRef.createUncommitted();
        IntRefTranlocal tranlocalIntValue3 = new IntRefTranlocal(intValue3, 0);

        WriteSet writeSet = WriteSet.create(tranlocalIntValue1, tranlocalIntValue2, tranlocalIntValue3);

        boolean result = policy.acquireLocks(writeSet, t);
        assertTrue(result);
        assertSame(t, intValue1.getLockOwner());
        assertSame(t, intValue2.getLockOwner());
        assertSame(t, intValue3.getLockOwner());
    }

    @Test
    public void singletonWriteSetWithNonFreeLock() {
        Transaction t1 = new DummyTransaction();
        Transaction t2 = new DummyTransaction();

        IntRef intValue = IntRef.createUncommitted();
        IntRefTranlocal tranlocalIntValue = new IntRefTranlocal(intValue, 0);


        WriteSet writeSet = new WriteSet(null, tranlocalIntValue);
        intValue.acquireLock(t2);

        boolean result = policy.acquireLocks(writeSet, t1);
        assertFalse(result);
        assertSame(t2, intValue.getLockOwner());
    }

    @Test
    public void nonSingletonWriteSetWithOneNonFreeLock() {
        Transaction t1 = new DummyTransaction();
        Transaction t2 = new DummyTransaction();

        IntRef intValue1 = IntRef.createUncommitted();
        IntRefTranlocal tranlocalIntValue = new IntRefTranlocal(intValue1, 0);

        IntRef intValue2 = IntRef.createUncommitted();
        IntRefTranlocal tranlocalIntValue2 = new IntRefTranlocal(intValue2, 0);

        IntRef intValue3 = IntRef.createUncommitted();
        IntRefTranlocal tranlocalIntValue3 = new IntRefTranlocal(intValue3, 0);

        WriteSet writeSet = WriteSet.create(tranlocalIntValue, tranlocalIntValue2, tranlocalIntValue3);
        intValue2.acquireLock(t2);

        boolean result = policy.acquireLocks(writeSet, t1);
        assertFalse(result);
        assertSame(t1, intValue1.getLockOwner());
        assertSame(t2, intValue2.getLockOwner());
        assertNull(intValue3.getLockOwner());
    }
}
