package org.multiverse.stms.alpha.instrumentation.integrationtest;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Test;
import org.multiverse.stms.alpha.instrumentation.asm.MetadataService;

/**
 * @author Peter Veentjer
 */
public class StackTest {

    @After
    public void tearDown() {
       // assertNoInstrumentationProblems();
    }

    @Test
    public void test() {
        MetadataService s = MetadataService.INSTANCE;

        Stack stack = new Stack();
        assertTrue(stack.isEmpty());
        assertEquals(0, stack.size());
    }

    @Test
    public void testPush() {
        Stack<Integer> stack = new Stack<Integer>();
        stack.push(1);
        assertFalse(stack.isEmpty());
        assertEquals(1, stack.size());
    }

    @Test
    public void popFromNonEmptyStack() {
        Stack<Integer> stack = new Stack<Integer>();
        stack.push(10);
        int result = (Integer) stack.pop();
        assertEquals(result, 10);
        assertEquals(0, stack.size());
    }

    @Test
    public void clear() {
        Stack<Integer> stack = new Stack<Integer>();
        stack.push(10);

        stack.clear();
        assertEquals(0, stack.size());
    }
}
