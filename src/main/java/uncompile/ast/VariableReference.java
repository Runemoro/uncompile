package uncompile.ast;

import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

public class VariableReference extends Expression {
    /* reference */ public VariableDeclaration declaration;

    public VariableReference(VariableDeclaration declaration) {
        this.declaration = declaration;
    }

    protected VariableReference() {
        declaration = null;
    }

    @Override
    public Type getType() {
        return declaration.type.toType();
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.print(declaration.name);
    }
}
