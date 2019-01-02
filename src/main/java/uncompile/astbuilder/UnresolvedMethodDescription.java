package uncompile.astbuilder;

import uncompile.metadata.*;

import java.util.Collections;
import java.util.List;

public class UnresolvedMethodDescription implements MethodDescription {
    private final ClassDescription declaringClass;
    private final String name;
    private final List<Type> parameterTypes;
    private final Type returnType;
    private final boolean isStatic;

    public UnresolvedMethodDescription(ClassDescription declaringClass, String name, List<Type> parameterTypes, Type returnType, boolean isStatic) {
        this.declaringClass = declaringClass;
        this.name = name;
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
        this.isStatic = isStatic;
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
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isSynchronized() {
        return false;
    }

    @Override
    public boolean isNative() {
        return false;
    }

    @Override
    public boolean isBridge() {
        return false;
    }

    @Override
    public boolean isSynthetic() {
        return false;
    }

    @Override
    public List<? extends ReferenceType> getExceptions() {
        return Collections.emptyList();
    }

    @Override
    public ClassDescription getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<? extends TypeParameterType> getTypeParameters() {
        return Collections.emptyList();
    }

    @Override
    public List<? extends Type> getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public Type getReturnType() {
        return returnType;
    }

    @Override
    public boolean isStatic() {
        return isStatic;
    }
}
