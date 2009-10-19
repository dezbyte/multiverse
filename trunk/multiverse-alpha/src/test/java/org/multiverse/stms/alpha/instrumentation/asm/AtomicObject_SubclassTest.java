package org.multiverse.stms.alpha.instrumentation.asm;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.TestUtils.testIncomplete;
import org.multiverse.api.GlobalStmInstance;
import org.multiverse.api.annotations.AtomicObject;
import org.multiverse.stms.alpha.AlphaStm;

/**
 * @author Peter Veentjer
 */
public class AtomicObject_SubclassTest {
    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        GlobalStmInstance.set(stm);
    }

    @Test
    public void subclassingAnAtomicObjectIsNotAllowed() {
        A a = new A();//force loading

        //this should fail.
        B b = new B();

        testIncomplete();
    }

    @AtomicObject
    public static class A {
        int x;
    }

    @AtomicObject
    public static class B extends A {
        int y;
    }

    @Test
    public void testAtomicObjectExtendingOtherNonAtomicObject() {
        long version = stm.getClockVersion();

        Banana banana = new Banana();

        assertEquals(version + 1, stm.getClockVersion());
        assertEquals(0, banana.getLength());
    }

    @Test
    public void methodOnAtomicObject() {
        Banana banana = new Banana();

        long version = stm.getClockVersion();

        banana.setLength(100);

        assertEquals(version + 1, stm.getClockVersion());
        assertEquals(100, banana.getLength());
    }

    @Test
    public void methodOnSuper() {
        Banana banana = new Banana();

        long version = stm.getClockVersion();

        banana.setWeight(100);

        assertEquals(version, stm.getClockVersion());
        assertEquals(100, banana.getWeight());
    }

    @Test
    public void constructorThatCallsParametrizedSuper() {
        int weight = 10;
        int length = 20;

        long version = stm.getClockVersion();

        Banana banana = new Banana(weight, length);

        assertEquals(version + 1, stm.getClockVersion());
        assertEquals(weight, banana.getWeight());
        assertEquals(length, banana.getLength());
    }

    static class Fruit {
        private int weight;

        public Fruit() {
            weight = 1;
        }

        public Fruit(int weight) {
            this.weight = weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }

        public int getWeight() {
            return weight;
        }
    }

    @AtomicObject
    static class Banana extends Fruit {
        private int length;

        Banana() {
            length = 0;
        }

        Banana(int length) {
            this.length = length;
        }

        Banana(int weight, int length) {
            super(weight);
            this.length = length;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }
    }
}