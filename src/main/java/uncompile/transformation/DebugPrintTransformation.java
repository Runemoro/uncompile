package uncompile.transformation;

import uncompile.ast.AstNode;

public class DebugPrintTransformation implements Transformation {
    @Override
    public void run(AstNode node) {
        System.out.println(node);
    }
}
