package uncompile.metadata;

import uncompile.ast.TypeNode;

public class WildcardType extends Type {
    public final TypeNode extendsBound;
    public final TypeNode superBound;

    public WildcardType(TypeNode extendsBound, TypeNode superBound) {
        this.extendsBound = extendsBound;
        this.superBound = superBound;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof WildcardType &&
               extendsBound.equals(((WildcardType) obj).extendsBound) &&
               superBound.equals(((WildcardType) obj).superBound);
    }

    @Override
    public int hashCode() {
        return 31 * extendsBound.hashCode() +
               31 * superBound.hashCode();
    }
}
