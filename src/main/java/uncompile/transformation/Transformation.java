package uncompile.transformation;

import uncompile.ast.AstNode;

public interface Transformation {
    void run(AstNode node);
}
