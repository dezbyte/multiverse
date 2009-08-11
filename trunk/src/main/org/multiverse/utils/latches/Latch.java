package org.multiverse.utils.latches;

import java.util.concurrent.TimeUnit;

/**
 * A structure that can be used as a waiting point. As long as it is closed, the thread waits (unless it is
 * interrupted or a timeout occurs). As soon as it opens, all waiting threads can pass. Threads that call the
 * wait after the Latch is opened, can continue. Once the Latch has been opened, it can never be closed.
 * <p/>
 * A Latch is threadsafe to use.
 *
 * @author Peter Veentjer.
 */
public interface Latch {

    /**
     * Opens the latch. If the latch already is open, the call is ignored. This makes the call
     * idempotent.
     */
    void open();

    /**
     * Return true if this Latch is open, false othewise.
     *
     * @return true if this Latch is open, false otherwise.
     */
    boolean isOpen();

    /**
     * Waits for this Latch to open. If the Latch already is open, the call continues.
     *
     * @throws InterruptedException if the waiting thread is interrupted.
     */
    void await() throws InterruptedException;

    /**
     * Waits for this Latch to open and while waiting it won't be interrupted. If the Latch
     * already is open, the call continues. If the thread is interrupted while waiting, the
     * interruptexception is dropped and the interrupt status is restored as soon as the method
     * returns.
     *
     * @throws UnsupportedOperationException if the implementation doesn't support this functionality.
     */
    void awaitUninterruptible();

    /**
     * Waits for this Latch to open or till a timeout occurs.
     *
     * @param timeout the maximum time to wait.
     * @param unit    the TimeUnit the timeout is expressed in
     * @throws InterruptedException          if the wait is interrupted. It depends on the implementation of this
     *                                       method really is responsive to interrupts.
     * @throws NullPointerException          if  unit is null
     * @throws UnsupportedOperationException if the implementation doesn't support this functionality.
     */
    void tryAwait(long timeout, TimeUnit unit) throws InterruptedException;
}
