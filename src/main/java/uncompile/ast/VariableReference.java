package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class VariableReference extends Expression {
    public VariableDeclaration declaration;

    public VariableReference(VariableDeclaration declaration) {
        this.declaration = declaration;
    }

    @Override
    public Type getType() {
        return declaration.type;
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
