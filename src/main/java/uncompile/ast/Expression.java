package uncompile.ast;

import uncompile.metadata.Type;

public abstract class Expression extends AstNode { // TODO: Expression<T extends Type>?
    public boolean needsSemicolon() {
        return true;
    }

    public abstract Type getType();
}
