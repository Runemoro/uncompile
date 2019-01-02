package uncompile.metadata;

public class NullType extends ReferenceType {
    public static final Type INSTANCE = new NullType();

    private NullType() {}

    @Override
    public ClassType getRawType() {
        return null; // TODO
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NullType;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
