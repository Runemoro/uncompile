package uncompile.astbuilder;

import uncompile.metadata.AccessLevel;
import uncompile.metadata.ClassDescription;
import uncompile.metadata.FieldDescription;
import uncompile.metadata.Type;

public class UnresolvedFieldDescription implements FieldDescription {
    private final ClassDescription declaringClass;
    private final String name;
    private final Type type;
    private final boolean isStatic;

    public UnresolvedFieldDescription(ClassDescription declaringClass, String name, Type type, boolean isStatic) {
        this.declaringClass = declaringClass;
        this.name = name;
        this.type = type;
        this.isStatic = isStatic;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ClassDescription getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.PUBLIC;
    }

    @Override
    public boolean isFinal() {
        return false;
    }

    @Override
    public boolean isVolatile() {
        return false;
    }

    @Override
    public boolean isTransient() {
        return false;
    }

    @Override
    public boolean isSynthetic() {
        return false;
    }
}
