package uncompile.ast;

import uncompile.metadata.Type;
import uncompile.metadata.WildcardType;
import uncompile.util.IndentingPrintWriter;

import javax.annotation.Nullable;

public class Wildcard extends TypeNode {
    @Nullable public TypeNode extendsBound;
    @Nullable public TypeNode superBound;

    public Wildcard(@Nullable TypeNode extendsBound, @Nullable TypeNode superBound) {
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

    @Override
    public Type toType() {
        return new WildcardType(extendsBound, superBound);
    }
}
