package uncompile.controlflow;

import uncompile.ast.Block;
import uncompile.transformation.*;

public final class ControlFlowGenerator {
    private static final Transformation[] TRANSFORMATIONS = {
            new RemoveSelfAssignmentsTransformation(),
            new GenerateConstructorCallsTransform(),
            new InlineSingleUseVariablesTransformation(),
            new InlineAliasVariablesTransformation()
    };

    private final ControlFlowGraph graph;

    public ControlFlowGenerator(ControlFlowGraph graph) {
        this.graph = graph;
    }

    public Block createCode() {
        for (ControlFlowNode node : this.graph.getNodes()) {
            for (Transformation transformation : TRANSFORMATIONS) {
                transformation.run(node.block);
            }
        }

        graph.calculateDominance();
        graph.calculateDominanceFrontier();
        System.out.println(graph);
        return new Block();
    }
}
