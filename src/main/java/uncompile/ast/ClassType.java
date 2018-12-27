package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class ClassType extends ObjectType {
    public static final ClassType OBJECT = new ClassType("java.lang.Object");
    public static final ClassType ENUM = new ClassType("java.lang.Enum");
    public static final ClassType STRING = new ClassType("java.lang.String");

    public final String fullName;

    public ClassType(String fullName) {
        this.fullName = fullName;
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
