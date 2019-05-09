package uncompile.controlflow;

import uncompile.ast.Expression;

public abstract class Jump {
    public abstract ControlFlowNode[] getTargets();

    public static class None extends Jump {
        @Override
        public ControlFlowNode[] getTargets() {
            return new ControlFlowNode[]{};
        }
    }

    public static class Unconditional extends Jump {
        public final ControlFlowNode nextNode;

        public Unconditional(ControlFlowNode nextNode) {
            this.nextNode = nextNode;
        }

        @Override
        public ControlFlowNode[] getTargets() {
            return new ControlFlowNode[]{nextNode};
        }
    }

    public static class Conditional extends Jump {
        public final Expression condition;
        public final ControlFlowNode trueNode;
        public final ControlFlowNode falseNode;

        public Conditional(Expression condition, ControlFlowNode trueNode, ControlFlowNode falseNode) {
            this.condition = condition;
            this.trueNode = trueNode;
            this.falseNode = falseNode;
        }

        @Override
        public ControlFlowNode[] getTargets() {
            return new ControlFlowNode[]{trueNode, falseNode};
        }
    }

    public static class Switch extends Jump {
        public final Expression expression;
        public final Expression[] cases;
        public final ControlFlowNode[] caseNodes;
        public final ControlFlowNode defaultNode;

        public Switch(Expression expression, Expression[] cases, ControlFlowNode[] caseNodes, ControlFlowNode defaultNode) {
            this.expression = expression;
            this.cases = cases;
            this.caseNodes = caseNodes;
            this.defaultNode = defaultNode;
        }

        @Override
        public ControlFlowNode[] getTargets() {
            ControlFlowNode[] nodes = new ControlFlowNode[caseNodes.length + 1];
            System.arraycopy(caseNodes, 0, nodes, 0, caseNodes.length);
            nodes[nodes.length - 1] = defaultNode;
            return nodes;
        }
    }
}
