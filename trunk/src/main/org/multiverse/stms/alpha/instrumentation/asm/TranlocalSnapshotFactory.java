package org.multiverse.stms.alpha.instrumentation.asm;

import org.multiverse.stms.alpha.Tranlocal;
import org.multiverse.stms.alpha.TranlocalSnapshot;
import static org.multiverse.stms.alpha.instrumentation.asm.AsmUtils.internalFormToDescriptor;
import org.objectweb.asm.Opcodes;
import static org.objectweb.asm.Type.getDescriptor;
import static org.objectweb.asm.Type.getInternalName;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import static java.lang.String.format;
import java.util.LinkedList;
import java.util.List;

/**
 * A Factory responsible for creating the TranlocalSnapshot classes for the
 * AtomicObject that need one.
 * <p/>
 * TranlocalSnapshotClassNodeFactory should not be reused.
 *
 * @author Peter Veentjer
 */
public class TranlocalSnapshotFactory implements Opcodes {
    private ClassNode original;
    private String tranlocalName;
    private String tranlocalSnapshotName;
    private MetadataService metadataService;

    public TranlocalSnapshotFactory(ClassNode original) {
        this.original = original;
        this.metadataService = MetadataService.INSTANCE;
        this.tranlocalName = metadataService.getTranlocalName(original);
        this.tranlocalSnapshotName = metadataService.getTranlocalSnapshotName(original);
    }

    public ClassNode create() {
        if (!metadataService.hasManagedInstanceFields(original)) {
            return null;
        }

        ClassNode result = new ClassNode();
        result.version = original.version;
        result.name = tranlocalSnapshotName;
        result.superName = getInternalName(TranlocalSnapshot.class);
        result.access = ACC_PUBLIC + ACC_FINAL + ACC_SYNTHETIC;
        result.sourceFile = original.sourceFile;
        result.sourceDebug = original.sourceDebug;
        result.methods.add(createConstructor());
        result.methods.add(createRestoreMethod());
        result.methods.add(createGetTranlocalMethod());
        result.fields.addAll(createFields());
        return result;
    }

    public List<FieldNode> createFields() {
        List<FieldNode> fields = new LinkedList<FieldNode>();

        FieldNode tranlocalField = new FieldNode(
                ACC_PRIVATE + ACC_FINAL + ACC_SYNTHETIC,
                "tranlocal",
                internalFormToDescriptor(tranlocalName),
                null,
                null);
        fields.add(tranlocalField);

        for (FieldNode originalField : metadataService.getManagedInstanceFields(original)) {
            FieldNode newField = new FieldNode(
                    ACC_PRIVATE + ACC_FINAL + ACC_SYNTHETIC,
                    originalField.name,
                    originalField.desc,
                    originalField.signature,
                    null);
            fields.add(newField);
        }

        return fields;
    }

    public MethodNode createConstructor() {
        MethodNode m = new MethodNode(
                ACC_PUBLIC + ACC_SYNTHETIC,
                "<init>",
                format("(%s)V", internalFormToDescriptor(tranlocalName)),
                null,
                new String[]{});

        m.visitVarInsn(ALOAD, 0);
        m.visitVarInsn(ALOAD, 1);
        m.visitFieldInsn(PUTFIELD, tranlocalSnapshotName, "tranlocal", internalFormToDescriptor(tranlocalName));

        for (FieldNode managedField : metadataService.getManagedInstanceFields(original)) {
            m.visitVarInsn(ALOAD, 0);
            m.visitVarInsn(ALOAD, 1);
            m.visitFieldInsn(GETFIELD, tranlocalName, managedField.name, managedField.desc);
            m.visitFieldInsn(PUTFIELD, tranlocalSnapshotName, managedField.name, managedField.desc);
        }

        m.visitInsn(RETURN);
        m.visitMaxs(0, 0);
        m.visitEnd();
        return m;
    }

    public MethodNode createGetTranlocalMethod() {
        MethodNode m = new MethodNode(
                ACC_PUBLIC + ACC_SYNTHETIC,
                "getTranlocal",
                format("()%s", getDescriptor(Tranlocal.class)),
                null,
                new String[]{});

        m.visitVarInsn(ALOAD, 0);
        m.visitFieldInsn(GETFIELD, tranlocalSnapshotName, "tranlocal", internalFormToDescriptor(tranlocalName));
        m.visitInsn(ARETURN);
        m.visitMaxs(0, 0);
        m.visitEnd();
        return m;
    }

    public MethodNode createRestoreMethod() {
        MethodNode m = new MethodNode(
                ACC_PUBLIC + ACC_SYNTHETIC,
                "restore",
                "()V",
                null,
                new String[]{});

        for (FieldNode managedField : metadataService.getManagedInstanceFields(original)) {
            m.visitVarInsn(ALOAD, 0);
            m.visitVarInsn(ALOAD, 0);
            m.visitFieldInsn(GETFIELD, tranlocalName, managedField.name, managedField.desc);
            m.visitFieldInsn(PUTFIELD, tranlocalSnapshotName, managedField.name, managedField.desc);
        }

        m.visitInsn(RETURN);
        m.visitMaxs(0, 0);
        m.visitEnd();
        return m;
    }
}
