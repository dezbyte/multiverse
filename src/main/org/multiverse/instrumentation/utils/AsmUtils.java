package org.multiverse.instrumentation.utils;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import static org.objectweb.asm.Type.*;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.*;
import java.util.List;

/**
 *
 */
public final class AsmUtils {

    /**
     * Checks if a type is a secondary (primitive long or double) type or not.
     *
     * @param desc a description of the type
     * @return true if it is a secondary type, false otherwise.
     */
    public static boolean isSecondaryType(String desc) {
        return desc.equals("J") || desc.equals("L");
    }


    public static boolean isSynthetic(FieldNode fieldNode) {
        return (fieldNode.access & Opcodes.ACC_SYNTHETIC) != 0;
    }

    public static boolean hasVisibleAnnotation(String typeDescriptor, Class annotationClass, ClassLoader classLoader) {
        if (typeDescriptor == null || annotationClass == null || classLoader == null)
            throw new NullPointerException();

        Type fieldType = getType(typeDescriptor);

        if (!isObjectType(fieldType))
            return false;

        ClassNode classNode = loadAsClassNode(classLoader, fieldType.getInternalName());
        return hasVisibleAnnotation(classNode, annotationClass);
    }

    /**
     * Checks if a ClassNode has the specified visible annotation.
     *
     * @param classNode      the ClassNode to check
     * @param anotationClass the Annotation class that is checked for.
     * @return true if classNode has the specified annotation, false otherwise.
     */
    public static boolean hasVisibleAnnotation(ClassNode classNode, Class anotationClass) {
        if (classNode == null || anotationClass == null)
            throw new NullPointerException();

        if (classNode.visibleAnnotations == null)
            return false;

        String annotationClassDesc = getDescriptor(anotationClass);

        for (AnnotationNode node : (List<AnnotationNode>) classNode.visibleAnnotations) {
            if (annotationClassDesc.equals(node.desc))
                return true;
        }

        return false;
    }

    public static boolean isObjectType(Type type) {
        return type.getDescriptor().startsWith("L");
    }

    public static String getShortClassName(ClassNode classNode) {
        String internalName = classNode.name;
        int lastIndex = internalName.lastIndexOf('/');
        if (lastIndex == -1)
            return internalName;

        return internalName.substring(lastIndex + 1);
    }

    public static String getPackagename(ClassNode classNode) {
        String internalName = classNode.name;
        int lastIndex = internalName.lastIndexOf('/');
        if (lastIndex == -1)
            return internalName;

        return internalName.substring(0, lastIndex).replace('/', '.');
    }

    public static void verify(File file) {
        verify(toBytes(file));
    }

    /**
     * Loads a file as a byte array.
     *
     * @param file the File to load.
     * @return the loaded bytearray.
     * @throws RuntimeException if an io error occurs.
     */
    public static byte[] toBytes(File file) {
        try {
            InputStream in = new FileInputStream(file);
            // Get the size of the file
            long length = file.length();

            // You cannot create an array using a long type.
            // It needs to be an int type.
            // Before converting to an int type, check
            // to ensure that file is not larger than Integer.MAX_VALUE.
            if (length > Integer.MAX_VALUE) {
                // File is too large
                throw new RuntimeException("file too large");
            }

            // Create the byte array to hold the data
            byte[] bytes = new byte[(int) length];

            // Read in the bytes
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length
                    && (numRead = in.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }

            // Ensure all the bytes have been read in
            if (offset < bytes.length) {
                throw new IOException("Could not completely read file " + file.getName());
            }

            in.close();
            return bytes;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if the bytecode is valid.
     *
     * @param bytes the bytecode to check.
     */
    public static void verify(byte[] bytes) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        CheckClassAdapter.verify(new ClassReader(bytes), false, pw);
        String msg = sw.toString();
        if (msg.length() > 0)
            throw new RuntimeException(msg);
    }

    public static void verify(ClassNode classNode) {
        verify(toBytecode(classNode));
    }

    /**
     * Loads a Class as ClassNode. The ClassLoader of the Class is used to retrieve a resource stream.
     *
     * @param clazz the Class to load as ClassNode.
     * @return the loaded ClassNode.
     */
    public static ClassNode loadAsClassNode(Class clazz) {
        return loadAsClassNode(clazz.getClassLoader(), getInternalName(clazz));
    }

    /**
     * Loads a Class as ClassNode.
     *
     * @param loader            the ClassLoader to get the resource stream of.
     * @param classInternalForm the internal name of the Class to load.
     * @return the loaded ClassNode.
     */
    public static ClassNode loadAsClassNode(ClassLoader loader, String classInternalForm) {
        if (loader == null || classInternalForm == null) throw new NullPointerException();

        String fileName = classInternalForm + ".class";
        InputStream is = loader.getResourceAsStream(fileName);

        try {
            ClassNode classNode = new ClassNode();
            ClassReader reader = new ClassReader(is);
            reader.accept(classNode, 0);
            return classNode;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads bytecode as a ClassNode.
     *
     * @param bytecode the bytecode to load.
     * @return the created ClassNode.
     */
    public static ClassNode loadAsClassNode(byte[] bytecode) {
        if (bytecode == null) throw new NullPointerException();

        ClassNode classNode = new ClassNode();
        ClassReader cr = new ClassReader(bytecode);
        cr.accept(classNode, 0);
        return classNode;
    }

    /**
     * Transforms a ClassNode to bytecode.
     *
     * @param classNode the ClassNode to transform to bytecode.
     * @return the transformed bytecode.
     * @throws NullPointerException if classNode is null.
     */
    public static byte[] toBytecode(ClassNode classNode) {
        if (classNode == null) throw new NullPointerException();

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(cw);
        return cw.toByteArray();
    }

    /**
     * Returns the bytecode for a specific Class.
     *
     * @param classname the name of the Class to load the bytecode for
     * @return the loaded bytecode
     * @throws IOException
     * @throws NullPointerException if classname is null.
     */
    public static byte[] toBytecode(String classname) throws IOException {
        if (classname == null) throw new NullPointerException();

        ClassReader reader = new ClassReader(classname);
        ClassWriter writer = new ClassWriter(reader, 0);
        reader.accept(writer, 0);
        return writer.toByteArray();
    }


    public static void writeToFixedTmpFile(Class clazz) throws IOException {
        byte[] bytecode = toBytecode(loadAsClassNode(clazz));
        writeToFixedTmpFile(bytecode);
    }

    public static void writeToFixedTmpFile(byte[] bytecode) throws IOException {
        File file = new File(getTmpDir(), "debug.class");
        writeToFile(file, bytecode);
    }

    public static String getTmpDir() {
        return System.getProperty("java.io.tmpdir");
    }

    public static File writeToFileInTmpDirectory(String filename, byte[] bytecode) throws IOException {
        File file = new File(getTmpDir(), filename);
        writeToFile(file, bytecode);
        return file;
    }

    public static void writeToTmpFile(byte[] bytecode) throws IOException {
        File file = File.createTempFile("foo", ".class");
        writeToFile(file, bytecode);
    }

    public static void writeToFile(File file, byte[] bytecode) throws IOException {
        if (file == null || bytecode == null) throw new NullPointerException();

        ensureExistingParent(file);

        OutputStream writer = new FileOutputStream(file);
        try {
            writer.write(bytecode);
        } finally {
            writer.close();
        }
    }

    public static void ensureExistingParent(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent.isDirectory())
            return;

        if (!parent.mkdirs())
            throw new IOException("Failed to make parent directories for file " + file);
    }

    //we don't want instances.
    private AsmUtils() {
    }
}