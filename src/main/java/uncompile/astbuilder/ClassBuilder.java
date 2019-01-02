package uncompile.astbuilder;

import org.objectweb.asm.*;
import uncompile.ClassProvider;
import uncompile.ast.Class;
import uncompile.metadata.ClassKind;
import uncompile.metadata.ClassType;
import uncompile.metadata.Type;
import uncompile.ast.*;
import uncompile.metadata.AccessLevel;
import uncompile.util.DescriptorReader;

public class ClassBuilder extends ClassVisitor {
    private final ClassProvider classProvider;
    private Class clazz = null;
    private String name = null;
    private String superName = null;
    private DescriptionProvider descriptionProvider;

    public ClassBuilder(ClassProvider classProvider, DescriptionProvider descriptionProvider) {
        super(Opcodes.ASM7);
        this.classProvider = classProvider;
        this.descriptionProvider = descriptionProvider;
    }

    public Class getResult() {
        return clazz;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.name = name;
        this.superName = superName;

        String packageName;
        String simpleClassName;
        int packageSeparator = name.lastIndexOf('/');
        if (packageSeparator == -1) {
            packageName = "";
            simpleClassName = name;
        } else {
            packageName = name.substring(0, packageSeparator).replace('/', '.');
            simpleClassName = name.substring(packageSeparator + 1).replace('/', '.');
        }

        clazz = new Class(
                packageName,
                simpleClassName,
                getAccessLevel(access),
                getClassKind(access),
                (access & Opcodes.ACC_STATIC) != 0,
                (access & Opcodes.ACC_FINAL) != 0,
                (access & Opcodes.ACC_ABSTRACT) != 0,
                (access & Opcodes.ACC_SYNTHETIC) != 0,
                new ClassReference(new ClassType(superName.replace('/', '.')))
        );

        for (String interfac : interfaces) {
            clazz.interfaces.add(new ClassReference(new ClassType(interfac.replace('/', '.'))));
        }

        descriptionProvider.addClassDescription(name, clazz);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        boolean isAnonymous = innerName == null;

        if (outerName == null) {
            outerName = name.substring(0, name.lastIndexOf('$'));
        }

        if (innerName == null) {
            innerName = name.substring(name.lastIndexOf('$') + 1);
        }

        if (outerName.equals(this.name)) {
            ClassBuilder innerClassBuilder = new ClassBuilder(classProvider, descriptionProvider);
            new ClassReader(classProvider.getClass(name)).accept(innerClassBuilder, ClassReader.EXPAND_FRAMES);
            innerClassBuilder.clazz.name = innerName;
            innerClassBuilder.clazz.outerClass = clazz;
            innerClassBuilder.clazz.accessLevel = getAccessLevel(access);
            innerClassBuilder.clazz.kind = getClassKind(access);
            innerClassBuilder.clazz.isStatic = (access & Opcodes.ACC_STATIC) != 0;
            innerClassBuilder.clazz.isFinal = (access & Opcodes.ACC_FINAL) != 0;
            innerClassBuilder.clazz.isAbstract = (access & Opcodes.ACC_ABSTRACT) != 0;
            innerClassBuilder.clazz.isSynthetic = (access & Opcodes.ACC_SYNTHETIC) != 0;
            innerClassBuilder.clazz.isAnonymous = isAnonymous;
            clazz.innerClasses.add(innerClassBuilder.clazz);
        }
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        Field field = new Field(
                name,
                clazz,
                TypeNode.fromType(new DescriptorReader(descriptor, 0).read()),
                getAccessLevel(access),
                (access & Opcodes.ACC_STATIC) != 0,
                (access & Opcodes.ACC_FINAL) != 0,
                (access & Opcodes.ACC_VOLATILE) != 0,
                (access & Opcodes.ACC_TRANSIENT) != 0,
                (access & Opcodes.ACC_SYNTHETIC) != 0
        );

        clazz.addField(field);

        descriptionProvider.addFieldDescription(this.name, name, descriptor, field);

        return null;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        Method method = new Method(
                name,
                clazz,
                getAccessLevel(access),
                (access & Opcodes.ACC_STATIC) != 0,
                (access & Opcodes.ACC_FINAL) != 0,
                (access & Opcodes.ACC_ABSTRACT) != 0,
                (access & Opcodes.ACC_SYNCHRONIZED) != 0,
                (access & Opcodes.ACC_NATIVE) != 0,
                (access & Opcodes.ACC_SYNTHETIC) != 0,
                (access & Opcodes.ACC_BRIDGE) != 0,
                null,
                null
        );

        String correctSignature = signature != null ? signature : descriptor;
        DescriptorReader r = new DescriptorReader(correctSignature, 0);

        if (correctSignature.charAt(r.pos++) != '(') {
            throw new IllegalStateException("Bad method signature: " + correctSignature);
        }

        // Parameters
        int index = 0;

        while (correctSignature.charAt(r.pos) != ')') {
            Type type = r.read();
            method.parameters.add(new VariableDeclaration(
                    TypeNode.fromType(type),
                    "par" + index,
                    (access & Opcodes.ACC_FINAL) != 0,
                    false,
                    true
            ));

            index++;
        }
        r.pos++;
        method.returnType = TypeNode.fromType(r.read());

        if (exceptions != null) {
            for (String exception : exceptions) {
                method.exceptions.add(new ClassReference(new ClassType(exception.replace('/', '.'))));
            }
        }

        clazz.addMethod(method);

        descriptionProvider.addMethodDescription(this.name, name, descriptor, method);

        return new MethodBuilder(method, this.name, superName, access, name, descriptor, signature, exceptions, descriptionProvider);
    }

    private ClassKind getClassKind(int access) {
        if ((access & Opcodes.ACC_ENUM) != 0) {
            return ClassKind.ENUM;
        }

        if ((access & Opcodes.ACC_INTERFACE) != 0) {
            return ClassKind.INTERFACE;
        }

        if ((access & Opcodes.ACC_ANNOTATION) != 0) {
            return ClassKind.ANNOTATION;
        }

        return ClassKind.CLASS;
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
