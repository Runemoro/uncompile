package uncompile.astbuilder;

import uncompile.ClassProvider;
import uncompile.util.DescriptorReader;
import uncompile.ast.Class;
import uncompile.ast.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import java.util.HashMap;
import java.util.Map;

public class ClassBuilder {
    private final ClassProvider classProvider;
    private final Map<String, Class> decompiledClasses = new HashMap<>();

    public ClassBuilder(ClassProvider classProvider) {
        this.classProvider = classProvider;
    }

    public Class getDecompiled(String className) {
        if (className.contains("/")) {
            throw new IllegalArgumentException("no slashes");
        }

        return decompiledClasses.computeIfAbsent(className, this::decompileClass);
    }

    private Class decompileClass(String className) {
        ClassNode classNode = classProvider.getClass(className);

        String packageName;
        String simpleClassName;

        int packageSeparator = classNode.name.lastIndexOf('/');
        if (packageSeparator == -1) {
            packageName = "";
            simpleClassName = className;
        } else {
            packageName = classNode.name.substring(0, packageSeparator).replace('/', '.');
            simpleClassName = classNode.name.substring(packageSeparator + 1).replace('/', '.');
        }

        Class clazz = new Class(
                packageName,
                simpleClassName,
                getAccessLevel(classNode.access),
                getClassKind(classNode.access),
                (classNode.access & Opcodes.ACC_STATIC) != 0,
                (classNode.access & Opcodes.ACC_FINAL) != 0,
                (classNode.access & Opcodes.ACC_ABSTRACT) != 0,
                (classNode.access & Opcodes.ACC_SYNTHETIC) != 0,
                new ClassReference(new ClassType(classNode.superName.replace('/', '.')))
        );

        for (String interfac : classNode.interfaces) {
            clazz.interfaces.add(new ClassReference(new ClassType(interfac)));
        }

        for (FieldNode fieldNode : classNode.fields) {
            Field field = new Field(
                    fieldNode.name,
                    clazz,
                    new DescriptorReader(fieldNode.desc, 0).read(),
                    getAccessLevel(fieldNode.access),
                    (fieldNode.access & Opcodes.ACC_STATIC) != 0,
                    (fieldNode.access & Opcodes.ACC_FINAL) != 0,
                    (fieldNode.access & Opcodes.ACC_VOLATILE) != 0,
                    (fieldNode.access & Opcodes.ACC_TRANSIENT) != 0,
                    (fieldNode.access & Opcodes.ACC_SYNTHETIC) != 0
            );

            clazz.addField(field);
        }

        for (MethodNode methodNode : classNode.methods) {
            Method method = new Method(
                    methodNode.name,
                    clazz,
                    getAccessLevel(methodNode.access),
                    (methodNode.access & Opcodes.ACC_STATIC) != 0,
                    (methodNode.access & Opcodes.ACC_FINAL) != 0,
                    (methodNode.access & Opcodes.ACC_ABSTRACT) != 0,
                    (methodNode.access & Opcodes.ACC_SYNCHRONIZED) != 0,
                    (methodNode.access & Opcodes.ACC_NATIVE) != 0,
                    (methodNode.access & Opcodes.ACC_SYNTHETIC) != 0,
                    (methodNode.access & Opcodes.ACC_BRIDGE) != 0,
                    null,
                    null
            );

            String signature = methodNode.signature != null ? methodNode.signature : methodNode.desc;
            DescriptorReader r = new DescriptorReader(signature, 0);

            if (signature.charAt(r.pos++) != '(') {
                throw new IllegalStateException("Bad method signature: " + signature);
            }

            // Parameters
            int index = 0;

            while (signature.charAt(r.pos) != ')') {
                Type type = r.read();

                ParameterNode parameterNode = methodNode.parameters != null && index < methodNode.parameters.size() ? methodNode.parameters.get(index) : null;
                method.parameters.add(new VariableDeclaration(
                        type,
                        parameterNode != null ? parameterNode.name : "par" + index, // TODO: check for conflicts
                        parameterNode != null && (parameterNode.access & Opcodes.ACC_FINAL) != 0,
                        parameterNode != null && (parameterNode.access & Opcodes.ACC_SYNTHETIC) != 0,
                        true
                ));

                index++;
            }
            r.pos++;
            method.returnType = r.read();

            for (String exception : methodNode.exceptions) {
                method.exceptions.add(new ClassReference(new ClassType(exception.replace('/', '.'))));
            }

            if (!(method.isAbstract && methodNode.instructions.size() == 0)) {
                MethodBuilder methodBuilder = new MethodBuilder(method, methodNode);
                methodNode.accept(methodBuilder);
                method.body = methodBuilder.finish();
            }

            clazz.addMethod(method);
        }

        return clazz;
    }
//
//    private Block decompileMethodBody(MethodNode methodNode) {
//        Block block = new Block();
//
//        ListIterator<AbstractInsnNode> iterator = methodNode.instructions.iterator();
//
//        while (iterator.hasNext()) {
//            AbstractInsnNode insn = iterator.next();
//            if (insn instanceof VarInsnNode) {
//                switch (insn.getOpcode()) {
//                    case Opcodes.AALOAD
//                    VarInsnNode varInsn = (VarInsnNode) insn;
//                }
//            }
//        }
//
//        return block;
//    }

    private Class.Kind getClassKind(int access) {
        if ((access & Opcodes.ACC_ENUM) != 0) {
            return Class.Kind.ENUM;
        }

        if ((access & Opcodes.ACC_INTERFACE) != 0) {
            return Class.Kind.INTERFACE;
        }

        if ((access & Opcodes.ACC_ANNOTATION) != 0) {
            return Class.Kind.ANNOTATION;
        }

        return Class.Kind.CLASS;
    }

    private AccessLevel getAccessLevel(int access) {
        if ((access & Opcodes.ACC_PUBLIC) != 0) {
            return AccessLevel.PUBLIC;
        }

        if ((access & Opcodes.ACC_PROTECTED) != 0) {
            return AccessLevel.PROTECTED;
        }

        if ((access & Opcodes.ACC_PRIVATE) != 0) {
            return AccessLevel.PRIVATE;
        }

        return AccessLevel.DEFAULT;
    }
}
