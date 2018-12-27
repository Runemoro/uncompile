package uncompile.transformation;

import uncompile.ast.Class;
import uncompile.ast.*;

import java.util.*;

/**
 * Inlines variable usages that have the same value as another variable.
 * <p>
 * This may result in unused variable declarations or declarations whose
 * scope is too large, so BringVariableDeclarationsCloserTransform should
 * be run after this transformation.
 */
public class InlineAliasVariablesTransform implements Transformation {
    @Override
    public void run(Class clazz) {
        for (Method method : clazz.methods) {
            if (method.body != null) {
                run(method); // TODO: several passes may be needed, see unalias below
            }
        }
    }

    private void run(Method method) {
        // TODO: Only constant variables are supported for now. Supporting all variables
        //  safely will need much more work.

        // Get constant variables
        Set<VariableDeclaration> constantVariables = new HashSet<>();
        Set<VariableDeclaration> variablesAssignedTwice = new HashSet<>();
        new AstVisitor() {
            @Override
            public void visit(Assignment assignment) {
                super.visit(assignment);

                if (assignment.left instanceof VariableReference) {
                    VariableDeclaration variable = ((VariableReference) assignment.left).declaration;
                    if (constantVariables.contains(variable)) {
                        variablesAssignedTwice.add(variable);
                    }

                    constantVariables.add(variable);
                }
            }
        }.visit(method.body);
        constantVariables.removeAll(variablesAssignedTwice);

        // Run the transformation
        new AstVisitor() {
            private Block currentBlock = null;
            private Map<VariableDeclaration, Set<VariableDeclaration>> aliasGroups = new HashMap<>();

            @Override
            public void visit(VariableReference variableReference) {
                variableReference.declaration = getAliasedTo(variableReference.declaration);
            }

            @Override
            public void visit(Assignment assignment) {
                if (assignment.left instanceof VariableReference) {
                    VariableDeclaration left = ((VariableReference) assignment.left).declaration;

                    if (constantVariables.contains(left) && assignment.right instanceof VariableReference) {
                        VariableDeclaration right = ((VariableReference) assignment.right).declaration;
                        alias(left, right);
                        return; // don't visit, to avoid creating self-assignments that aren't removed by any transformation yet
                    }

                    unalias(left);
                }

                super.visit(assignment);
            }

            @Override
            public void visit(Block block) {
                Block oldBlock = currentBlock;
                currentBlock = block;
                super.visit(block);
                currentBlock = oldBlock;
            }

            private void alias(VariableDeclaration from, VariableDeclaration to) {
                unalias(from);

                Set<VariableDeclaration> toSet = aliasGroups.computeIfAbsent(to, k -> new LinkedHashSet<>());
                toSet.add(to); // no effect on LinkedHashSet order if already present
                toSet.add(from);
                aliasGroups.put(from, toSet);
                aliasGroups.put(to, toSet);
            }

            private void unalias(VariableDeclaration variable) {
                // Remove variable from its alias group. The remaining variables are
                // still equal and will correctly point to the second variable that was
                // added to the group.
                // TODO: are several passes needed because of this?
                aliasGroups.getOrDefault(variable, Collections.emptySet()).remove(variable);
                aliasGroups.remove(variable);
            }

            private VariableDeclaration getAliasedTo(VariableDeclaration variable) {
                Set<VariableDeclaration> toSet = aliasGroups.get(variable);
                if (toSet != null && !toSet.isEmpty()) {
                    // This will return the first element added to that set, which
                    // is the first alias that still remains.
                    return toSet.iterator().next();
                }

                return variable;
            }
        }.visit(method.body);
    }
}
