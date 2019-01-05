package uncompile.transformation;

import uncompile.ast.Class;

public class AstTransformations {
    private static Transformation[] transformations = {
            new RemoveUnusedLabelsTransform(),
            new GenerateControlFlowTransform(),
            new SimplifyControlFlowTransform(),
            new RemoveUnusedLabelsTransform(),
            new FixInnerClassesTransform(),
            new InlineAliasVariablesTransform(),
            new BringVariableDeclarationsCloserTransform(),
            new RemoveUnusedAssignmentsTransform(),
            new InlineSingleUseVariablesTransform(),
            new FlipIfElseTransform(),
            new AddImportsTransform()
    };

    public static void run(Class decompiled) {
        for (Transformation transformation : transformations) {
            transformation.run(decompiled);
        }
    }
}
