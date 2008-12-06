package org.codehaus.multiverse.transaction;


/**
 *
 * A Transaction can be compared to a Hibernate Session. 
 *
 * A Transaction is not threadsafe and should not be shared between threads, unless it is safely moved from
 * one thread to another. todo: think about the future need for a threadlocal to store the transaction
 *
 * @author Peter Veentjer
 */
public interface Transaction {

    /**
     * Attaches the object to the Transaction. Multiple calls to the attach method on the same transaction of the
     * same object are ignored. Roots that are not fresh can be attached again (call will be ignored). If other
     * objects can be reached from the object, they are attached as well.
     *
     * This method is not threadsafe.
     *
     * @param root the object to attach
     * @throws NullPointerException     if object is null.
     * @throws IllegalStateException    if this Transaction is not in the started state.
     * @throws IllegalArgumentException if object is not an object that can be stored in this transaction. A specific
     *                                  type for the root, can't be used, because compile time the instrumentation
     *                                  of additional interfaces has not been done.
     * @throws BadTransactionException if root, or any other citizen that can be reached through this,
     *                                  root,  already is attached to a different transaction
     * @return the address of the object. If the transaction is not committed, this address is not valid.
     */
    long attachAsRoot(Object root);

    /**
     * Reads an object.
     *
     * This method is not threadsafe.
     *
     * @param handle the handle to the object.
     * @return the Object at the pointer.
     * @throws NoSuchObjectException
     */
    Object read(long handle);

    /**
     * todo
     *
     * @param handle the pointer of the object to remove.
     */
    void unmarkAsRoot(long handle);

    /**
     * todo
     */
    void unmarkAsRoot(Object root);

    /**
     * Returns the status of this Transaction
     *
     * This method is threadsafe.
     *
     * @return the status of this Transaction
     */
    TransactionStatus getStatus();

    /**
     * Commits the changes to STM. Multiple commits are ignored.
     *
     * This method is not threadsafe.
     *
     * @throws IllegalStateException if the Transaction is already aborted.
     * @throws WriteConflictException if a WriteConflict happens when
     * @throws BadTransactionException if one or more of the reachable objects is bound to a different transaction.
     */
    void commit();

    /**
     * Rolls back the transaction. Multiple calls on the abort method are ignored.
     *
     * This method is not threadsafe.
     *
     * @throws IllegalStateException if the Transaction already has been committed.
     */
    void abort();
}
