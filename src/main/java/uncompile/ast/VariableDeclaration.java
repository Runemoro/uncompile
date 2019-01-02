package uncompile.ast;

import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

import java.util.Objects;

public class VariableDeclaration extends VariableReference {
    public TypeNode type;
    public String name;
    public boolean isFinal;
    public boolean isSynthetic;
    public boolean isParameter;

    public VariableDeclaration(TypeNode type, String name, boolean isFinal, boolean isSynthetic, boolean isParameter) {
        declaration = this;
        this.type = Objects.requireNonNull(type);
        this.name = name;
        this.isFinal = isFinal;
        this.isSynthetic = isSynthetic;
        this.isParameter = isParameter;
    }

    @Override
    public Type getType() {
        return type.toType();
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        if (isFinal) {
            w.append("final ");
        }

        if (isSynthetic) {
            w.append("/* synthetic */ ");
        }

        type.append(w);
        w.append(" ");
        w.append(name);
    }
}
