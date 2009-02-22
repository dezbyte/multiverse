package org.codehaus.multiverse.multiversionedstm.growingheap;

import org.codehaus.multiverse.core.NoSuchObjectException;
import org.codehaus.multiverse.multiversionedstm.DehydratedStmObject;
import org.codehaus.multiverse.multiversionedstm.MultiversionedHeap;
import org.codehaus.multiverse.multiversionedstm.MultiversionedHeapSnapshot;
import org.codehaus.multiverse.multiversionedstm.utils.DefaultListenerSupport;
import org.codehaus.multiverse.multiversionedstm.utils.ListenerSupport;
import org.codehaus.multiverse.multiversionedstm.utils.MultiversionedHeapSnapshotChain;
import org.codehaus.multiverse.util.iterators.ArrayIterator;
import org.codehaus.multiverse.util.iterators.ResetableIterator;
import org.codehaus.multiverse.util.latches.Latch;

import static java.lang.String.format;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Default {@link org.codehaus.multiverse.multiversionedstm.MultiversionedHeap} implementation that is able to grow.
 *
 * @author Peter Veentjer.
 */
public final class SmartGrowingMultiversionedHeap implements MultiversionedHeap {

    private final MultiversionedHeapSnapshotChain<MultiversionedHeapSnapshotImpl> snapshotChain =
            new MultiversionedHeapSnapshotChain<MultiversionedHeapSnapshotImpl>(new MultiversionedHeapSnapshotImpl());

    private final GrowingMultiversionedHeapStatistics statistics = new GrowingMultiversionedHeapStatistics();

    private final ListenerSupport listenerSupport = new DefaultListenerSupport();

    public SmartGrowingMultiversionedHeap() {
    }

    public MultiversionedHeapSnapshotChain<MultiversionedHeapSnapshotImpl> getSnapshotChain() {
        return snapshotChain;
    }

    public GrowingMultiversionedHeapStatistics getStatistics() {
        return statistics;
    }

    public MultiversionedHeapSnapshot getActiveSnapshot() {
        return snapshotChain.getHead();
    }

    public MultiversionedHeapSnapshot getSnapshot(long version) {
        return snapshotChain.get(version);
    }

    /**
     * Commits the changes to the heap. This is a convenience method that allows for a varargs.
     */
    public CommitResult commit(MultiversionedHeapSnapshot startSnapshot, DehydratedStmObject... changes) {
        return commit(startSnapshot, new ArrayIterator<DehydratedStmObject>(changes));
    }

    public int getSnapshotAliveCount() {
        return snapshotChain.getAliveCount();
    }

    public CommitResult commit(MultiversionedHeapSnapshot startSnapshot, ResetableIterator<DehydratedStmObject> changes) {
        if (changes == null) throw new NullPointerException();

        statistics.commitTotalCount.incrementAndGet();
        statistics.commitNonBlockingStatistics.incEnterCount();

        if (!changes.hasNext()) {
            //if there are no changes to write to the heap, the transaction was readonly and we are done.
            statistics.commitReadonlyCount.incrementAndGet();
            return CommitResult.createReadOnly(snapshotChain.getHead());
        }
        CreateNewSnapshotResult createNewSnapshotResult;

        DehydratedStmObject object1 = changes.next();
        if (!changes.hasNext()) {
            boolean anotherTransactionDidCommit;
            do {
                MultiversionedHeapSnapshotImpl activeSnapshot = snapshotChain.getHead();
                createNewSnapshotResult = activeSnapshot.createNew(object1, startSnapshot);

                if (!createNewSnapshotResult.success) {
                    statistics.commitWriteConflictCount.incrementAndGet();
                    return CommitResult.createWriteConflict();
                }

                anotherTransactionDidCommit = !snapshotChain.compareAndAdd(
                        activeSnapshot,
                        createNewSnapshotResult.snapshot);

                if (anotherTransactionDidCommit) {
                    //if another transaction also did a commit while we were creating a new snapshot, we
                    //have to try again. We need to reset the changes iterator for that.
                    statistics.commitNonBlockingStatistics.incFailureCount();
                }
            } while (anotherTransactionDidCommit);
        } else {
            boolean anotherTransactionDidCommit;
            do {
                MultiversionedHeapSnapshotImpl activeSnapshot = snapshotChain.getHead();
                createNewSnapshotResult = activeSnapshot.createNew(changes, startSnapshot);

                if (!createNewSnapshotResult.success) {
                    statistics.commitWriteConflictCount.incrementAndGet();
                    return CommitResult.createWriteConflict();
                }

                anotherTransactionDidCommit = !snapshotChain.compareAndAdd(
                        activeSnapshot,
                        createNewSnapshotResult.snapshot);

                if (anotherTransactionDidCommit) {
                    //if another transaction also did a commit while we were creating a new snapshot, we
                    //have to try again. We need to reset the changes iterator for that.
                    statistics.commitNonBlockingStatistics.incFailureCount();
                    changes.reset();
                }
            } while (anotherTransactionDidCommit);

        }
        //the commit was a success, so lets update the state.
        //todo: this could be done on another thread, since it doesn't matter who  wakes up the listeners.
        //todo: this would help scalability.
        listenerSupport.wakeupListeners(
                createNewSnapshotResult.snapshot.getVersion(),
                createNewSnapshotResult.getHandles());
        statistics.commitSuccessCount.incrementAndGet();
        statistics.committedStoreCount.addAndGet(createNewSnapshotResult.handles.length);
        return CommitResult.createSuccess(createNewSnapshotResult.snapshot, createNewSnapshotResult.handles.length);
    }

    public void listen(Latch latch, long[] handles, long transactionVersion) {
        if (latch == null) throw new NullPointerException();

        //if there are no addresses to listen to, open the latch and return.
        //todo: is this desirable behavior? Do you event want to allow this situation?
        if (handles.length == 0) {
            latch.open();
            return;
        }

        statistics.listenNonBlockingStatistics.incEnterCount();

        //the latch only needs to be registered once.
        boolean latchIsAdded = false;
        boolean success;
        do {
            MultiversionedHeapSnapshotImpl snapshot = snapshotChain.getHead();
            for (long handle : handles) {
                if (hasUpdate(snapshot, handle, latch, transactionVersion))
                    return;

                //if the latch is opened
                if (latch.isOpen())
                    return;
            }

            if (!latchIsAdded) {
                listenerSupport.addListener(transactionVersion + 1, handles, latch);
                latchIsAdded = true;
            }

            success = snapshot == snapshotChain.getHead();
            if (!success)
                statistics.listenNonBlockingStatistics.incFailureCount();
        } while (!success);
    }

    private boolean hasUpdate(MultiversionedHeapSnapshotImpl currentSnapshot, long handle, Latch latch, long transactionVersion) {
        long version = currentSnapshot.readVersion(handle);
        if (version == -1) {
            //the object doesn't exist anymore.
            //lets open the latch so that is can be cleaned up.
            latch.open();
            throw new NoSuchObjectException(handle, currentSnapshot.version);
        } else if (version > transactionVersion) {
            //woohoo, we have an overwrite, the latch can be opened, and we can end this method.
            //The event the transaction is waiting for already has occurred.
            latch.open();
            return true;
        } else {
            //The event the transaction is waiting for, hasn't occurred yet. So lets register the latch.
            //The latch is always registered on the Snapshot of the transaction version.
            return false;
        }
    }

    /**
     * GrowingHeap specific HeapSnapshot implementation.
     * <p/>
     * Class is completely immutable.
     */
    private class MultiversionedHeapSnapshotImpl implements MultiversionedHeapSnapshot {
        private final HeapNode root;
        private final long version;

        MultiversionedHeapSnapshotImpl() {
            version = 0;
            root = null;
        }

        MultiversionedHeapSnapshotImpl(HeapNode root, long version) {
            this.root = root;
            this.version = version;
        }

        public long getVersion() {
            return version;
        }

        public long[] getRoots() {
            throw new RuntimeException();
        }

        public DehydratedStmObject read(long handle) {
            statistics.readCount.incrementAndGet();

            if (handle == 0 || root == null)
                return null;

            HeapNode node = root.find(handle);
            return node == null ? null : node.getContent();
        }

        public long readVersion(long handle) {
            if (root == null || handle == 0)
                return -1;

            HeapNode node = root.find(handle);
            return node == null ? -1 : node.getContent().getVersion();
        }

        /**
         * Creates a new HeapSnapshotImpl based on the current HeapSnapshot and all the changes. Since
         * each HeapSnapshot  is immutable, a new HeapSnapshotImpl is created instead of modifying the
         * existing one.
         *
         * @param changes      an iterator over all the DehydratedStmObject that need to be written
         * @param startVersion the version of the heap when the transaction, that wants to commits, began. This
         *                     information is required for write conflict detection.
         * @return the created HeapSnapshot or null of there was a write conflict.
         */
        public CreateNewSnapshotResult createNew(Iterator<DehydratedStmObject> changes, MultiversionedHeapSnapshot startSnapshot) {
            long commitVersion = version + 1;

            HeapNode newRoot = root;
            //the snapshot the transaction sees when it begin. All changes it made on objects, are on objects
            //loaded from this version.

            //the handles of objects that are written to the heap
            List<Long> storedHandles = new LinkedList<Long>();

            for (; changes.hasNext();) {
                DehydratedStmObject change = changes.next();

                long handle = change.getHandle();

                if (hasWriteConflict(handle, startSnapshot))
                    return CreateNewSnapshotResult.createWriteConflict();

                storedHandles.add(handle);

                change.setVersion(commitVersion);
                if (newRoot == null)
                    newRoot = new DefaultHeapNode(change, null, null);
                else
                    newRoot = newRoot.createNew(change);
            }

            MultiversionedHeapSnapshotImpl newSnapshot = new MultiversionedHeapSnapshotImpl(newRoot, commitVersion);
            return CreateNewSnapshotResult.createSuccess(newSnapshot, storedHandles);
        }

        public CreateNewSnapshotResult createNew(DehydratedStmObject change, MultiversionedHeapSnapshot startSnapshot) {
            long commitVersion = version + 1;

            HeapNode newRoot = root;

            long handle = change.getHandle();

            if (hasWriteConflict(handle, startSnapshot))
                return CreateNewSnapshotResult.createWriteConflict();

            change.setVersion(commitVersion);
            if (newRoot == null)
                newRoot = new DefaultHeapNode(change, null, null);
            else
                newRoot = newRoot.createNew(change);

            MultiversionedHeapSnapshotImpl newSnapshot = new MultiversionedHeapSnapshotImpl(newRoot, commitVersion);
            return CreateNewSnapshotResult.createSuccess(newSnapshot, handle);
        }


        /**
         * Checks if the current HeapSnapshot has a write conflict at the specified handle with the startSnapshot.
         * <p/>
         * A write conflict on some handle happens when a different transaction made a change after the start
         * of the transaction and the commit of the transaction. Field that are changes but no overwritten by
         * the transaction, are not checked at the moment.
         *
         * @param handle        the handle of the Object to check.
         * @param startSnapshot the Snapshot of the Heap when the transaction that wants to write
         * @return true if there was a write conflict, false otherwise.
         */
        private boolean hasWriteConflict(long handle, MultiversionedHeapSnapshot startSnapshot) {
            //if the current snapshot is the same as the startSnapshot, other transactions didn't update the heap.
            //so we know that there is no commit conflict. Further checking is not needed.
            if (startSnapshot == this)
                return false;

            long previousVersion = startSnapshot.readVersion(handle);

            //if the object is new to the heap, there is no commit conflict.
            if (previousVersion == -1)
                return false;

            //the version of object in the current snapshot.
            long newVersion = this.readVersion(handle);

            //the object was in the heap at some point in time and not visible from the current MultiversionedHeapSnapshot anymore,
            //because it has been removed, so there is a commit conflict
            if (newVersion == -1)
                return true;

            //the object is found in both snapshots. If the version still is the same, there is no commit conflict.
            //if the version isn't the same, it means that it has been updated by a different transaction in the
            //mean while.
            return newVersion != previousVersion;
        }

        public String toString() {
            return format("Snapshot(version=%s)", version);
        }
    }

    private static class CreateNewSnapshotResult {
        MultiversionedHeapSnapshotImpl snapshot;
        long[] handles;
        boolean success;

        static CreateNewSnapshotResult createSuccess(MultiversionedHeapSnapshotImpl snapshot, long... handles) {
            CreateNewSnapshotResult snapshotResult = new CreateNewSnapshotResult();
            snapshotResult.snapshot = snapshot;
            snapshotResult.success = true;
            snapshotResult.handles = handles;
            return snapshotResult;
        }

        static CreateNewSnapshotResult createSuccess(MultiversionedHeapSnapshotImpl snapshot, List<Long> handles) {
            CreateNewSnapshotResult snapshotResult = new CreateNewSnapshotResult();
            snapshotResult.snapshot = snapshot;
            snapshotResult.success = true;
            snapshotResult.handles = new long[handles.size()];
            Iterator<Long> it = handles.iterator();
            for (int k = 0; k < snapshotResult.handles.length; k++)
                snapshotResult.handles[k] = it.next();


            return snapshotResult;
        }

        static CreateNewSnapshotResult createWriteConflict() {
            CreateNewSnapshotResult snapshotResult = new CreateNewSnapshotResult();
            snapshotResult.success = false;
            return snapshotResult;
        }

        long[] getHandles() {
            return handles;
        }
    }
}