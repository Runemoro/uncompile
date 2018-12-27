package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class Wildcard extends Type {
    public Type extendsBound;
    public Type superBound;

    public Wildcard(Type extendsBound, Type superBound) {
        this.extendsBound = extendsBound;
        this.superBound = superBound;
    }


    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append("?");

        if (extendsBound != null) {
            w.append(" extends ");
            extendsBound.append(w);
        }

        if (superBound != null) {
            w.append(" super ");
            superBound.append(w);
        }
    }
}
