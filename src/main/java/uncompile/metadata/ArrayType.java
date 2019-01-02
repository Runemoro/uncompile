package uncompile.metadata;

public class ArrayType extends ClassType {
    private Type componentType;

    public ArrayType(Type componentType) {
        super(componentType + "[]"); // TODO
        this.componentType = componentType;
    }

    public Type getComponentType() {
        return componentType;
    }

    @Override
    public ClassType getRawType() {
        return new ClassType(getComponentType() + "[]"); // TODO
    }

    public String toString() {
        return componentType + "[]";
    }
}
