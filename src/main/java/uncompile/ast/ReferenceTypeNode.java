package uncompile.ast;

import uncompile.metadata.ReferenceType;

public abstract class ReferenceTypeNode extends TypeNode {
    @Override
    public abstract ReferenceType toType();
}
