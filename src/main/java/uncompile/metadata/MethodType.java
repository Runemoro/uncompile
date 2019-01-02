package uncompile.metadata;

import java.util.List;

public interface MethodType {
    ClassDescription getDeclaringClass();

    String getName();

    List<? extends TypeParameterType> getTypeParameters();

    List<? extends Type> getParameterTypes();

    Type getReturnType();

    boolean isStatic();
}
