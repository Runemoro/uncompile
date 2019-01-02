package uncompile.metadata;

import java.util.List;

public interface ClassDescription {
    String getFullName();

    AccessLevel getAccessLevel();

    boolean isFinal();

    boolean isAbstract();

    boolean isSynthetic();

    ReferenceType getSuperClass();

    List<? extends Type> getInterfaces();

    List<? extends FieldDescription> getFields();

    List<? extends MethodDescription> getMethods();

    default ClassType getType() {
        return new ClassType(getFullName());
    }

    boolean isComplete();
}
