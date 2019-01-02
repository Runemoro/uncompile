package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

// TODO: An inner class could be named the same as a package... But supporting that
//  is probably not worth the effort
public class ClassType extends ObjectType {
    public static final ClassType OBJECT = new ClassType("java.lang.Object");
    public static final ClassType ENUM = new ClassType("java.lang.Enum");
    public static final ClassType STRING = new ClassType("java.lang.String");
    public static final ClassType CLASS = new ClassType("java.lang.Class");

    public final String fullName;

    public ClassType(String fullName) {
        this.fullName = fullName;
    }

    public String getInternalName() {
        return fullName.replace('.', '/');
    }

    @Override
    public ClassType getRawType() {
        return this;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append(fullName);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this ||
               obj instanceof ClassType &&
               fullName.equals(((ClassType) obj).fullName);
    }

    @Override
    public int hashCode() {
        return fullName.hashCode();
    }
}
