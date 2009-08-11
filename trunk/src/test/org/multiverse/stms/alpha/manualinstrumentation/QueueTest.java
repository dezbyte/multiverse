package org.multiverse.stms.alpha.manualinstrumentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.utils.TransactionThreadLocal.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class QueueTest {

    @Before
    public void setUp() {
        setThreadLocalTransaction(null);
    }

    @Test
    public void testConstruction() {
        Queue queue = new Queue();
        assertEquals(0, queue.size());
        assertTrue(queue.isEmpty());
    }

    @Test
    public void complexScenario() {
        Queue<String> queue = new Queue<String>();
        queue.push("1");
        queue.push("2");

        assertEquals("1", queue.take());

        queue.push("3");

        assertEquals("2", queue.take());
        assertEquals("3", queue.take());
    }
}
