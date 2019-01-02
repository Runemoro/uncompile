package uncompile.metadata;

public class TypeParameterType extends ReferenceType {
    public final String name;
    public final ReferenceType extendsBound;
    public final Object declarationScope;

    public TypeParameterType(String name, ReferenceType extendsBound, Object declarationScope) {
        this.name = name;
        this.extendsBound = extendsBound;
        this.declarationScope = declarationScope;
    }

    @Override
    public ClassType getRawType() {
        return extendsBound != null ? extendsBound.getRawType() : ClassType.OBJECT;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TypeParameterType &&
               name.equals(((TypeParameterType) obj).name) &&
               extendsBound.equals(((TypeParameterType) obj).extendsBound) &&
               declarationScope.equals(((TypeParameterType) obj).declarationScope);
    }

    @Override
    public int hashCode() {
        return 31 * 31 * name.hashCode() +
               31 * extendsBound.hashCode() +
               declarationScope.hashCode();
    }
}
