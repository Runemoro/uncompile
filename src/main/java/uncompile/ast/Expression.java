package uncompile.ast;

public abstract class Expression extends AstNode {
    public boolean needsSemicolon() {
        return true;
    }

    public abstract Type getType();
}
