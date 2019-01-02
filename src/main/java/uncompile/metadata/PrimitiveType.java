package uncompile.metadata;

import java.util.Objects;

public final class PrimitiveType extends Type {
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

    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this);
    }
}
