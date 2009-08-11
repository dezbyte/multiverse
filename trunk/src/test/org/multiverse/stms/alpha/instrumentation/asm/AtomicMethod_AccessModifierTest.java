package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.Test;
import static org.multiverse.TestUtils.assertIsActive;
import org.multiverse.api.annotations.AtomicMethod;
import static org.multiverse.utils.TransactionThreadLocal.getThreadLocalTransaction;

public class AtomicMethod_AccessModifierTest {

    //@After
    //public void tearDown(){
    //    assertNoInstrumentationProblems();
    //}

    public static void assertTransactionWorking() {
        assertIsActive(getThreadLocalTransaction());
    }

    @Test
    public void privateMethod() {
        PrivateMethod method = new PrivateMethod();
        method.doIt();
    }

    public static class PrivateMethod {

        @AtomicMethod
        private void doIt() {
            assertTransactionWorking();
        }
    }

    @Test
    public void publicMethod() {
        PublicMethod method = new PublicMethod();
        method.doIt();
    }

    public static class PublicMethod {

        @AtomicMethod
        public void doIt() {
            assertTransactionWorking();
        }
    }

    @Test
    public void protectedMethod() {
        ProtectedMethod method = new ProtectedMethod();
        method.doIt();
    }

    public static class ProtectedMethod {

        @AtomicMethod
        protected void doIt() {
            assertTransactionWorking();
        }
    }

    @Test
    public void packageFriendlyMethod() {
        PackageFriendlyMethod method = new PackageFriendlyMethod();
        method.doIt();
    }

    public static class PackageFriendlyMethod {

        @AtomicMethod
        void doIt() {
            assertTransactionWorking();
        }
    }

    @Test
    public void abstractMethodFails() {
        //todo
    }

    @Test
    public void synchronizedMethodGivesNoProblems() {
        SynchronizedMethod method = new SynchronizedMethod();
        method.doIt();
    }

    public class SynchronizedMethod {

        @AtomicMethod
        public synchronized void doIt() {
            assertTransactionWorking();
        }
    }
}

