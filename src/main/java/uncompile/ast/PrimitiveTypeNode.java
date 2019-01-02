package uncompile.ast;

import uncompile.metadata.PrimitiveType;
import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

public class PrimitiveTypeNode extends TypeNode {
    public PrimitiveType primitiveType;

    public PrimitiveTypeNode(PrimitiveType primitiveType) {
        this.primitiveType = primitiveType;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append(primitiveType.name);
    }

    @Override
    public Type toType() {
        return primitiveType;
    }
}
