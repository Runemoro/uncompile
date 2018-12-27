package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class VariableDeclaration extends VariableReference {
    public Type type;
    public String name;
    public boolean isFinal;
    public boolean isSynthetic;
    public boolean isParameter;

    public VariableDeclaration(Type type, String name, boolean isFinal, boolean isSynthetic, boolean isParameter) {
        super(null);
        declaration = this;
        this.type = type;
        this.name = name;
        this.isFinal = isFinal;
        this.isSynthetic = isSynthetic;
        this.isParameter = isParameter;
    }

    @Override
    public Type getType() {
        return type;
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

    public boolean equals(Object obj) {
        // If ever this is changed, a new "Block declarationScope"
        // field would need to be added so that similarly named
        // variables are not equal.
        return this == obj;
    }
}
