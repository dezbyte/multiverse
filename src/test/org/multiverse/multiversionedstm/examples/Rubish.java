package org.multiverse.multiversionedstm.examples;

import org.multiverse.instrumentation.utils.AsmUtils;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;

public class Rubish {

    public static void main(String[] args) throws IOException {
        ClassNode classNode = AsmUtils.loadAsClassNode(IntegerValue.class);
        AsmUtils.writeToFixedTmpFile(AsmUtils.toBytecode(classNode));
    }

}