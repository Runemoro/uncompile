package uncompile.astbuilder;

import uncompile.metadata.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UnresolvedClassDescription implements ClassDescription {
    private final String name;
    private final List<FieldDescription> fields = new ArrayList<>();
    private final List<MethodDescription> methods = new ArrayList<>();

    public UnresolvedClassDescription(String name) {
        this.name = name;
    }

    @Override
    public String getFullName() {
        return name;
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
    public boolean isSynthetic() {
        return false;
    }

    @Override
    public ReferenceType getSuperClass() {
        return ClassType.OBJECT;
    }

    @Override
    public List<? extends Type> getInterfaces() {
        return Collections.emptyList();
    }

    @Override
    public List<FieldDescription> getFields() {
        return fields;
    }

    @Override
    public List<MethodDescription> getMethods() {
        return methods;
    }

    @Override
    public boolean isComplete() {
        return false;
    }
}
