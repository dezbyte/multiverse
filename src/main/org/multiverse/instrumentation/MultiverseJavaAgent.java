package org.multiverse.instrumentation;

import org.multiverse.api.TmEntity;
import org.multiverse.instrumentation.utils.AsmUtils;
import static org.multiverse.instrumentation.utils.AsmUtils.hasVisibleAnnotation;
import static org.multiverse.instrumentation.utils.AsmUtils.loadAsClassNode;
import org.objectweb.asm.tree.ClassNode;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;

public class MultiverseJavaAgent {

    //method that a javaagent must implement.
    public static void premain(String agentArgs, Instrumentation inst) throws UnmodifiableClassException {
        System.out.println("Starting the Multiverse JavaAgent");

        inst.addTransformer(new Phase1ClassFileTransformer());
        //inst.addTransformer(new Phase2ClassFileTransformer());
    }

    //responsible for transforming and generated the dematerializable/dematerialized classes
    public static class Phase1ClassFileTransformer implements ClassFileTransformer {

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            try {

                ClassNode classNode = loadAsClassNode(classfileBuffer);

                if (!hasVisibleAnnotation(classNode, TmEntity.class)) {
                    return null;
                }

                System.out.printf("Transforming class %s\n", className);

                DematerializedClassBuilder dematerializedClassBuilder = new DematerializedClassBuilder(classNode, loader);
                ClassNode dematerialized = dematerializedClassBuilder.create();

                if (MultiverseClassLoader.INSTANCE == null)
                    new MultiverseClassLoader();

                MultiverseClassLoader.INSTANCE.defineClass(dematerialized);

                TmEntityClassTransformer t = new TmEntityClassTransformer(classNode, dematerialized, loader);
                ClassNode materialized = t.create();
                return AsmUtils.toBytecode(materialized);
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                throw ex;
            } catch (Error e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    //responsible for transforming access to fields of dematerializable objects. 
    public static class Phase2ClassFileTransformer implements ClassFileTransformer {

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            return null;
        }
    }
}
