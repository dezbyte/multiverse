package org.multiverse;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;
import org.multiverse.api.*;
import org.multiverse.api.exceptions.NoCommittedDataFoundException;
import org.multiverse.multiversionedstm.MaterializedObject;
import org.multiverse.multiversionedstm.MultiversionedStm;
import org.multiverse.multiversionedstm.examples.ExampleIntegerValue;
import org.multiverse.util.latches.Latch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class TestUtils {

    public static void assertHasHandle(Object item, Handle handle) {
        if (item == null) {
            assertNull(handle);
            return;
        }

        assertTrue(item instanceof MaterializedObject);
        assertSame(handle, ((MaterializedObject) item).getHandle());
    }

    public static void assertSameHandle(Object item1, Object item2) {
        if (item1 == null) {
            assertTrue(item2 == null);
            return;
        }
        assertNotNull(item2);
        assertTrue(item1 instanceof MaterializedObject);
        assertTrue(item2 instanceof MaterializedObject);
        assertSame(((MaterializedObject) item1).getHandle(), ((MaterializedObject) item2).getHandle());
    }

    public static void assertIsActive(Transaction t) {
        assertEquals(TransactionState.active, t.getState());
    }

    public static void assertIsCommitted(Transaction t) {
        assertEquals(TransactionState.committed, t.getState());
    }

    public static void assertIsAborted(Transaction t) {
        assertEquals(TransactionState.aborted, t.getState());
    }

    public static boolean equals(Object o1, Object o2) {
        if (o1 == null)
            return o2 == null;

        return o1.equals(o2);
    }

    public static void assertWriteCount(MultiversionedStm stm, long expected) {
        assertEquals(expected, stm.getStatistics().getWriteCount());
    }

    public static void assertRematerializedCount(MultiversionedStm stm, long expected) {
        assertEquals(expected, stm.getStatistics().getMaterializedCount());
    }

    public static void assertGlobalVersion(MultiversionedStm stm, long expectedVersion) {
        assertEquals(expectedVersion, stm.getGlobalVersion());
    }

    public static void assertTransactionCommittedCount(MultiversionedStm stm, long commitCount) {
        assertEquals(commitCount, stm.getStatistics().getTransactionCommittedCount());
    }

    public static void assertTransactionReadonlyCount(MultiversionedStm stm, long readonlyCount) {
        assertEquals(readonlyCount, stm.getStatistics().getTransactionReadonlyCount());
    }

    public static void assertTransactionAbortedCount(MultiversionedStm stm, long abortedCount) {
        assertEquals(abortedCount, stm.getStatistics().getTransactionAbortedCount());
    }

    public static void assertTransactionRetriedCount(MultiversionedStm stm, long retriedCount) {
        assertEquals(retriedCount, stm.getStatistics().getTransactionRetriedCount());
    }

    public static void assertMaterializedCount(MultiversionedStm stm, long expectedMaterializedCount) {
        assertEquals(expectedMaterializedCount, stm.getStatistics().getMaterializedCount());
    }

    public static void assertIntegerValue(MultiversionedStm stm, Handle<ExampleIntegerValue> handle, int value) {
        Transaction t = stm.startTransaction();
        ExampleIntegerValue i = t.read(handle);
        assertEquals(value, i.get());
        t.commit();
    }

    public static void assertNoCommits(MultiversionedStm stm, Handle handle) {
        Transaction t = stm.startTransaction();
        try {
            t.read(handle);
            fail();
        } catch (NoCommittedDataFoundException ex) {
            assertTrue(true);
        } finally {
            t.abort();
        }
    }

    public static <T> T commitAndRead(Stm stm, T item) {
        return read(stm, commit(stm, item));
    }

    public static <T> Handle<T> commit(T item) {
        return commit(GlobalStmInstance.getInstance(), item);
    }

    public static <T> Handle<T> commit(Stm stm, T item) {
        Transaction t = stm.startTransaction();
        Handle<T> handle = t.attach(item);
        t.commit();
        return handle;
    }

    public static <T> T read(Stm stm, Handle<T> handle) {
        Transaction t = stm.startTransaction();
        T result = t.read(handle);
        t.commit();
        return result;
    }


    public static boolean randomBoolean() {
        return randomInteger(10) % 2 == 0;
    }

    public static int randomInteger(int max) {
        return (int) Math.round(Math.random() * max);
    }

    public static long randomLong(long i, int diff) {
        return (long) (i + (diff * (Math.random() - 0.5)));
    }


    public static <E> void assertAsListContent(Iterator<E> it, E... expectedItems) {
        List<E> expectedList = Arrays.asList(expectedItems);
        List<E> foundList = asList(it);
        assertEquals(expectedList, foundList);
    }

    public static <E> void assertAsSetContent(Iterator<E> it, E... expectedItems) {
        Set<E> expectedSet = new HashSet(Arrays.asList(expectedItems));
        Set<E> foundSet = new HashSet(asList(it));
        assertEquals(expectedSet, foundSet);
    }

    private static <E> List asList(Iterator<E> it) {
        List<E> result = new LinkedList<E>();
        for (; it.hasNext();)
            result.add(it.next());
        return result;
    }

    public static void sleepRandomMs(long maxMs) {
        if (maxMs == 0)
            return;

        sleep((long) (Math.random() * maxMs));
        Thread.yield();
    }

    public static void sleep(long ms) {
        if (ms == 0)
            return;

        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.interrupted();
            throw new RuntimeException(ex);
        }
    }

    public static void startAll(TestThread... threads) {
        for (Thread thread : threads)
            thread.start();
    }

    public static void joinAll(TestThread... threads) {
        for (TestThread thread : threads) {
            System.out.println("Joining " + thread.getName());
            try {
                thread.join();
                if (thread.getThrowable() != null) {
                    thread.getThrowable().printStackTrace();
                    fail();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            System.out.println("Joined successfully " + thread.getName());
        }
    }

    public static void assertIsOpen(Latch latch, boolean isOpen) {
        assertEquals(isOpen, latch.isOpen());
    }

    public static String readText(File errorOutputFile) {
        try {
            StringBuffer sb = new StringBuffer();
            BufferedReader reader = new BufferedReader(new FileReader(errorOutputFile));

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            return sb.toString();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}