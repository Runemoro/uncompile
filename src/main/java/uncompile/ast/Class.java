package uncompile.ast;

import uncompile.metadata.AccessLevel;
import uncompile.metadata.ClassDescription;
import uncompile.metadata.ClassType;
import uncompile.metadata.ReferenceType;
import uncompile.util.IndentingPrintWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Class extends AstNode implements ClassDescription {
    public enum Kind {
        CLASS("class"),
        ENUM("enum"),
        INTERFACE("interface"),
        ANNOTATION("annotation");

        private final String name;

        Kind(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public String packageName;
    public String name;
    public AccessLevel accessLevel;
    public Kind kind;
    public boolean isStatic;
    public boolean isFinal;
    public boolean isAbstract;
    public boolean isSynthetic;
    public ReferenceTypeNode superType;
    public List<ReferenceTypeNode> interfaces = new ArrayList<>();
    public Class outerClass = null;
    public List<Field> fields = new ArrayList<>();
    public List<Method> methods = new ArrayList<>();
    public List<Class> innerClasses = new ArrayList<>();
    public List<ClassType> imports = new ArrayList<>(); // only if outerClass == null
    public boolean isAnonymous = false;

    public Class(String packageName, String name, AccessLevel accessLevel, Kind kind, boolean isStatic, boolean isFinal, boolean isAbstract, boolean isSynthetic, ReferenceTypeNode superType) {
        this.packageName = packageName;
        this.name = name;
        this.accessLevel = accessLevel;
        this.kind = kind;
        this.isStatic = isStatic;
        this.isFinal = isFinal;
        this.isAbstract = isAbstract;
        this.isSynthetic = isSynthetic;
        this.superType = superType;
    }

    public boolean isNormalClass() {
        return kind == Kind.CLASS;
    }

    public boolean isEnum() {
        return kind == Kind.ENUM;
    }

    public boolean isInterface() {
        return kind == Kind.INTERFACE;
    }

    public boolean isAnnotation() {
        return kind == Kind.ANNOTATION;
    }

    public ClassType getClassType() {
        return new ClassType(getFullName());
    }

    @Override
    public String getFullName() {
        if (outerClass != null) {
            return outerClass.getFullName() + "." + name;
        } else {
            return packageName.isEmpty() ? name : packageName + "." + name;
        }
    }

    @Override
    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    @Override
    public boolean isFinal() {
        return isFinal;
    }

    @Override
    public boolean isAbstract() {
        return isAbstract;
    }

    @Override
    public boolean isSynthetic() {
        return isSynthetic;
    }

    @Override
    public ReferenceType getSuperClass() {
        return superType.toType();
    }

    @Override
    public List<ReferenceType> getInterfaces() {
        return interfaces
                .stream()
                .map(ReferenceTypeNode::toType)
                .collect(Collectors.toList());
    }

    @Override
    public List<Field> getFields() {
        return fields;
    }

    @Override
    public List<Method> getMethods() {
        return methods;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    public void addField(Field field) {
        fields.add(field);
    }

    public void addMethod(Method method) {
        methods.add(method);
    }

    public Field getFieldByName(String name) {
        for (Field field : fields) {
            if (field.name.equals(name)) {
                return field;
            }
        }

        return null;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        if (outerClass == null) {
            if (!packageName.isEmpty()) {
                w.append("package ")
                 .append(packageName)
                 .append(";");
                w.println();
                w.println();
            }

            if (!imports.isEmpty()) {
                for (ClassType impor : imports) {
                    w.append("import ")
                     .append(impor.getFullName())
                     .append(";");
                    w.println();
                }
                w.println();
            }
        }

        // Modifiers
        if (accessLevel != AccessLevel.DEFAULT) {
            w.append(accessLevel.toString()).append(" ");
        }

        if (isStatic) {
            w.append("static ");
        }

        if (isFinal) {
            w.append("final ");
        }

        if (isAbstract) {
            w.append("abstract ");
        }

        if (isSynthetic) {
            w.append("/* synthetic */ ");
        }

        // Kind
        w.append(kind.toString())
         .append(" ");

        // Name
        w.append(name);

        // Extends
        if (!superType.toType().getRawType().equals(ClassType.OBJECT) && !(isEnum() && superType.toType().getRawType().equals(ClassType.ENUM))) {
            w.append(" extends ");
            superType.append(w);
        }

        // Implements
        if (!interfaces.isEmpty()) {
            w.append(" implements ");
            boolean first = true;
            for (TypeNode interfac : interfaces) {
                if (!first) {
                    w.append(", ");
                }
                first = false;

                w.append(interfac);
            }
        }

        w.append(" {");
        w.indent();
        w.println();

        // Inner classes
        for (Class innerClass : innerClasses) {
            innerClass.append(w);
            w.println();
            w.println();
        }

        // Fields
        for (Field field : fields) {
            field.append(w);
            w.println();
        }
        if (!fields.isEmpty()) {
            w.println();
        }

        // Methods
        boolean first = true;
        for (Method method : methods) {
            if (!first) {
                w.println();
                w.println();
            }
            first = false;

            method.append(w);
        }

        w.unindent();
        w.println();
        w.print("}");
    }
}
