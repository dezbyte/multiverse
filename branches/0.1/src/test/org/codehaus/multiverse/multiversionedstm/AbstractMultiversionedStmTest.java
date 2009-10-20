package org.codehaus.multiverse.multiversionedstm;

import org.codehaus.multiverse.AbstractTransactionTest;
import org.codehaus.multiverse.core.Transaction;
import org.codehaus.multiverse.multiversionedstm.examples.Person;
import org.codehaus.multiverse.multiversionedstm.growingheap.GrowingMultiversionedHeap;

public abstract class AbstractMultiversionedStmTest extends AbstractTransactionTest<MultiversionedStm, MultiversionedStm.MultiversionedTransaction> {
    protected GrowingMultiversionedHeap heap;

    public MultiversionedStm createStm() {
        heap = new GrowingMultiversionedHeap();
        return new MultiversionedStm(heap);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        System.out.println(stm.getStatistics());
        System.out.println(heap.getStatistics());
        System.out.println("heap.snapshots.alive " + heap.getSnapshotAliveCount());
    }

    public void atomicIncAge(long handle, int newage) {
        MultiversionedStm.MultiversionedTransaction t = stm.startTransaction();
        Person person = (Person) t.read(handle);
        person.setAge(newage);
        t.commit();
    }

    public long atomicInsertPerson(String name, int age) {
        return atomicInsert(new Person(age, name));
    }

    public void assertStmVersionHasNotChanged() {
        assertEquals(transaction.getVersion(), stm.getCurrentVersion());
    }

    public void assertStmActiveVersion(long expected) {
        assertEquals(expected, stm.getCurrentVersion());
    }

    public void assertTransactionHasNoWrites() {
        assertEquals(0, transaction.getWriteCount());
    }

    public void assertTransactionWriteCount(long expected) {
        assertEquals(expected, transaction.getWriteCount());
    }

    public void assertTransactionHasNoReadsFromHeap() {
        assertEquals(0, transaction.getReadFromHeapCount());
    }

    public void assertTransactionReadsFromHeap(long expected) {
        assertEquals(expected, transaction.getReadFromHeapCount());
    }

    public void assertActualVersion(long ptr, long expectedVersion) {
        long foundVersion = heap.getActiveSnapshot().readVersion(ptr);
        assertEquals(expectedVersion, foundVersion);
    }

    public void assertCurrentStmVersion(long expectedVersion) {
        assertEquals(expectedVersion, stm.getCurrentVersion());
    }

    public void assertCommitCount(long expected) {
        assertEquals(expected, stm.getStatistics().getTransactionsCommitedCount());
    }

    public void assertStartedCount(long expected) {
        assertEquals(expected, stm.getStatistics().getTransactionsStartedCount());
    }

    public void assertAbortedCount(long expected) {
        assertEquals(expected, stm.getStatistics().getTransactionsAbortedCount());
    }

    public void assertHasHandleAndTransaction(StmObject object, long expectedPtr, Transaction expectedTrans) {
        assertNotNull(object);
        assertEquals(expectedPtr, object.___getHandle());
        assertEquals(expectedTrans, object.___getTransaction());
    }

    public void assertHasHandleAndTransaction(StmObject object, Transaction expectedTrans) {
        assertNotNull(object);
        assertFalse(0 == object.___getHandle());
        assertEquals(expectedTrans, object.___getTransaction());
    }

    public void assertHasHandle(long expectedPtr, StmObject... citizens) {
        for (StmObject citizen : citizens)
            assertEquals("Pointer is not the same", expectedPtr, citizen.___getHandle());
    }

    public void assertHasTransaction(Transaction expected, StmObject... citizens) {
        for (StmObject citizen : citizens)
            assertSame("Transaction is not the same", expected, citizen.___getTransaction());
    }

    public void assertHasNoTransaction(StmObject... citizens) {
        for (StmObject citizen : citizens)
            assertNull("Transaction should be null", citizen.___getTransaction());
    }

    public void assertHeapContains(long handle, long expectedVersion, DehydratedStmObject expected) {
        DehydratedStmObject found = heap.getSnapshot(expectedVersion).read(handle);
        assertEquals("Content doesn't match", expected, found);
    }

    public void assertHeapContainsNow(long handle, long expectedVersion, DehydratedStmObject expected) {
        assertEquals("Versions don't match. -1 indicates no cell with given address",
                expectedVersion,
                heap.getActiveSnapshot().readVersion(handle));
        DehydratedStmObject found = heap.getSnapshot(expectedVersion).read(handle);
        assertEquals("Content doesn't match", expected, found);
    }
}