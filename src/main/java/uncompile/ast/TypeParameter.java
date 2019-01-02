package uncompile.ast;

import uncompile.metadata.TypeParameterType;
import uncompile.util.IndentingPrintWriter;

public class TypeParameter extends ReferenceTypeNode {
    public String name;
    public ReferenceTypeNode extendsBound;
    public Expression declarationScope;

    public TypeParameter(String name, ReferenceTypeNode extendsBound, Expression declarationScope) {
        this.name = name;
        this.extendsBound = extendsBound;
        this.declarationScope = declarationScope;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append(name);

        if (extendsBound != null) {
            w.append(" extends ");
            extendsBound.append(w);
        }
    }

    @Override
    public TypeParameterType toType() {
        return new TypeParameterType(name, extendsBound.toType(), declarationScope);
    }
}
