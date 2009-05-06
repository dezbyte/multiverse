package org.multiverse.instrumentation.javaagent;

import org.multiverse.api.Dematerializable;
import org.multiverse.api.Transaction;
import static org.multiverse.instrumentation.javaagent.InstrumentationUtils.*;
import org.multiverse.multiversionedstm.DematerializedObject;
import org.multiverse.multiversionedstm.MultiversionedHandle;
import static org.objectweb.asm.Type.getDescriptor;

import static java.lang.String.format;
import java.lang.reflect.Field;

public class DematerializedClassBuilder extends ClassBuilder {

    private final Class materializedClass;

    public DematerializedClassBuilder(Class materializedClass) {
        this.materializedClass = materializedClass;
        this.classNode.version = V1_5;
        this.classNode.access = ACC_PUBLIC | ACC_FINAL;
        this.classNode.name = getInternalNameOfDematerializedClass(materializedClass);

        addInterface(Dematerializable.class);

        addPublicFinalField("handle", MultiversionedHandle.class);

        for (Field field : materializedClass.getFields()) {
            if (field.getType().isAnnotationPresent(Dematerializable.class)) {
                addPublicFinalField(field.getName(), MultiversionedHandle.class);
            } else {
                addPublicFinalField(field.getName(), field.getType());
            }
        }

        addMethod(new ConstructorBuilder());
        addMethod(new GetHandleMethodBuilder());
        addMethod(new RematerializeMethodBuilder());
    }

    private class ConstructorBuilder extends MethodBuilder {

        private ConstructorBuilder() {
            setName("<init>");
            setDescription(format("(L%s;)V", getClassInternalName()));

            ALOAD(0);
            INVOKESPECIAL(getConstructor(Object.class));
            ALOAD(0);
            ALOAD(1);

            //todo: de rest van de constructor.

            /*
             6 invokevirtual #26 <org/multiverse/multiversionedstm/examples/IntegerValue.getHandle>
             9 putfield #28 <org/multiverse/multiversionedstm/examples/IntegerValue$DematerializedIntegerValue.handle>
            12 aload_0
            13 aload_1
            14 invokestatic #32 <org/multiverse/multiversionedstm/examples/IntegerValue.access$300>
            17 putfield #34 <org/multiverse/multiversionedstm/examples/IntegerValue$DematerializedIntegerValue.value>
            */
            RETURN();
        }
    }

    private class GetHandleMethodBuilder extends MethodBuilder {

        private GetHandleMethodBuilder() {
            initWithInterfaceMethod(getMethod(DematerializedObject.class, "getHandle"));

            ALOAD(0);
            PUTFIELD(getClassInternalName(), "handle", MultiversionedHandle.class);
            ARETURN();
        }
    }

    private class RematerializeMethodBuilder extends MethodBuilder {

        private RematerializeMethodBuilder() {
            initWithInterfaceMethod(getMethod(DematerializedObject.class, "rematerialize", Transaction.class));

            NEW(materializedClass);
            DUP();
            ALOAD(0);
            ALOAD(1);
            String desc = format("(L%s;%s)V", getClassInternalName(), getDescriptor(Transaction.class));
            INVOKESPECIAL(materializedClass, "<init>", desc);
            ARETURN();
        }
    }
}
