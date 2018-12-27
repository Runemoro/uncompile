package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class PrimitiveType extends Type {
    public static final PrimitiveType BOOLEAN = new PrimitiveType("boolean");
    public static final PrimitiveType BYTE = new PrimitiveType("byte");
    public static final PrimitiveType SHORT = new PrimitiveType("short");
    public static final PrimitiveType INT = new PrimitiveType("int");
    public static final PrimitiveType LONG = new PrimitiveType("long");
    public static final PrimitiveType CHAR = new PrimitiveType("char");
    public static final PrimitiveType VOID = new PrimitiveType("void");
    public static final PrimitiveType FLOAT = new PrimitiveType("float");
    public static final PrimitiveType DOUBLE = new PrimitiveType("double");

    public final String name;

    private PrimitiveType(String name) {
        this.name = name;
    }


    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append(name);
    }
}
