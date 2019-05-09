package uncompile.transformation;

import uncompile.ast.Class;

public class AstTransformations {
    private static final Transformation[] TRANSFORMATIONS = {
            new DebugPrintTransformation(),
            new RemoveUnusedLabelsTransformation(),
            new FixInnerClassesTransformation(),
            new InlineAliasVariablesTransformation(),
            new RemoveUnusedAssignmentsTransformation(),
            new BringVariableDeclarationsCloserTransformation(),
            new FlipIfElseTransformation(),
            new MergeNestedIfsTransformation(),
            new AddImportsTransformation()
    };

    public static void run(Class decompiled) {
        for (Transformation transformation : TRANSFORMATIONS) {
            transformation.run(decompiled);
        }
    }
}
