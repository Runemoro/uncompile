package uncompile.metadata;

public interface FieldDescription {
    String getName();

    ClassDescription getDeclaringClass();

    Type getType();

    boolean isStatic();

    AccessLevel getAccessLevel();

    boolean isFinal();

    boolean isVolatile();

    boolean isTransient();

    boolean isSynthetic();
}
