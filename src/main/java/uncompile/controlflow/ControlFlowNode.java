package uncompile.controlflow;

import uncompile.ast.Block;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ControlFlowNode implements Comparable<ControlFlowNode> {
    public final int index;
    public boolean visited = false;

    // Graph
    public final List<ControlFlowNode> incoming = new ArrayList<>();
    public final List<ControlFlowNode> outgoing = new ArrayList<>();

    // Dominance
    public ControlFlowNode immediateDominator = null;
    public final List<ControlFlowNode> immediateDominating = new ArrayList<>();
    public final Set<ControlFlowNode> dominanceFrontier = new LinkedHashSet<>();

    // Code
    public Block block = null;
    public Jump jump = null;


    public ControlFlowNode(int index) {
        this.index = index;
    }

    public void setJump(Jump jump) {
        if (this.jump != null) {
            throw new IllegalStateException("jump already set");
        }

        this.jump = jump;

        for (ControlFlowNode node : jump.getTargets()) {
            outgoing.add(node);
            node.incoming.add(this);
        }
    }

    public final boolean isReachable() {
        return immediateDominator != null;
    }

    public final void traversePreOrder(Function<ControlFlowNode, Iterable<ControlFlowNode>> children, Consumer<ControlFlowNode> visitor) {
        if (visited) {
            return;
        }

        visited = true;
        visitor.accept(this);

        for (ControlFlowNode child : children.apply(this)) {
            child.traversePreOrder(children, visitor);
        }
    }

    public final void traversePostOrder(Function<ControlFlowNode, Iterable<ControlFlowNode>> children, Consumer<ControlFlowNode> visitor) {
        if (visited) {
            return;
        }

        visited = true;

        for (ControlFlowNode child : children.apply(this)) {
            child.traversePostOrder(children, visitor);
        }

        visitor.accept(this);
    }

    public final boolean dominates(ControlFlowNode node) {
        ControlFlowNode current = node;

        while (current != null) {
            if (current == this) {
                return true;
            }
            current = current.immediateDominator;
        }

        return false;
    }

    @Override
    public int compareTo(ControlFlowNode o) {
        return Integer.compare(index, o.index);
    }
}
