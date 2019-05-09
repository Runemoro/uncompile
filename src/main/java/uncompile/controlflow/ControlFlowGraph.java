package uncompile.controlflow;

import uncompile.ast.Statement;
import uncompile.util.IndentingPrintWriter;

import java.io.StringWriter;
import java.util.*;

public final class ControlFlowGraph {
    public ControlFlowNode entryPoint = null;
    private final Set<ControlFlowNode> nodes = new LinkedHashSet<>();
    private int nodeIndex = 0;

    public final ControlFlowNode getEntryPoint() {
        return entryPoint;
    }

    public final Set<ControlFlowNode> getNodes() {
        return nodes;
    }

    public final void resetVisited() {
        for (ControlFlowNode node : nodes) {
            node.visited = false;
        }
    }

    public final void calculateDominance() {
        ControlFlowNode entryPoint = getEntryPoint();

        entryPoint.immediateDominator = entryPoint;

        boolean[] changed = {true};

        while (changed[0]) {
            changed[0] = false;
            resetVisited();

            entryPoint.traversePreOrder(
                    controlFlowNode -> controlFlowNode.outgoing,
                    (ControlFlowNode b) -> {
                        if (b == entryPoint) {
                            return;
                        }

                        ControlFlowNode newImmediateDominator = null;

                        for (ControlFlowNode p : b.incoming) {
                            if (p.visited && p != b) {
                                newImmediateDominator = p;
                                break;
                            }
                        }

                        if (newImmediateDominator == null) {
                            throw new IllegalStateException("Could not compute new immediate dominator!");
                        }

                        for (ControlFlowNode p : b.incoming) {
                            if (p != b && p.immediateDominator != null) {
                                newImmediateDominator = findCommonDominator(p, newImmediateDominator);
                            }
                        }

                        if (b.immediateDominator != newImmediateDominator) {
                            b.immediateDominator = newImmediateDominator;
                            changed[0] = true;
                        }
                    }
            );
        }

        entryPoint.immediateDominator = null;

        for (ControlFlowNode node : nodes) {
            ControlFlowNode immediateDominator = node.immediateDominator;
            if (immediateDominator != null) {
                immediateDominator.immediateDominating.add(node);
            }
        }
    }

    public final void calculateDominanceFrontier() {
        resetVisited();

        getEntryPoint().traversePostOrder(
                controlFlowNode -> controlFlowNode.immediateDominating,
                n -> {
                    Set<ControlFlowNode> dominanceFrontier = n.dominanceFrontier;

                    dominanceFrontier.clear();

                    for (ControlFlowNode s : n.outgoing) {
                        if (s.immediateDominator != n) {
                            dominanceFrontier.add(s);
                        }
                    }

                    for (ControlFlowNode dominated : n.immediateDominating) {
                        for (ControlFlowNode p : dominated.dominanceFrontier) {
                            if (p.immediateDominator != n) {
                                dominanceFrontier.add(p);
                            }
                        }
                    }
                }
        );
    }

    public static ControlFlowNode findCommonDominator(ControlFlowNode a, ControlFlowNode b) {
        Set<ControlFlowNode> path1 = new LinkedHashSet<>();

        ControlFlowNode node1 = a;
        ControlFlowNode node2 = b;

        while (node1 != null && path1.add(node1)) {
            node1 = node1.immediateDominator;
        }

        while (node2 != null) {
            if (path1.contains(node2)) {
                return node2;
            }
            node2 = node2.immediateDominator;
        }

        throw new IllegalStateException("No common dominator found!");
    }

    public ControlFlowNode createNode() {
        ControlFlowNode node = new ControlFlowNode(nodeIndex++);
        nodes.add(node);

        if (entryPoint == null) {
            entryPoint = node;
        }

        return node;
    }

    @Override
    public final String toString() {
        StringWriter sw = new StringWriter();
        IndentingPrintWriter output = new IndentingPrintWriter(sw);

        output.println("digraph g {");
        output.indent();

        for (ControlFlowNode node : nodes) {
            if (node != entryPoint && node.immediateDominator == null) {
                continue;
            }

            output.println("\"" + ("node" + node.index) + "\" [");
            output.indent();

            StringBuilder content = new StringBuilder();
            boolean first = true;
            for (Statement s : node.block) {
                if (!first) {
                    content.append("\n");
                }
                first = false;
                content.append(s);
            }

            Map<ControlFlowNode, String> colors = new HashMap<>();

            if (node.jump instanceof Jump.Conditional) {
                content.append("\n").append("Condition: " + ((Jump.Conditional) node.jump).condition);
                colors.put(((Jump.Conditional) node.jump).trueNode, "green");
                colors.put(((Jump.Conditional) node.jump).falseNode, "red");
            }

            output.println("label = \"" + escape(content.toString()) + "\\l\"");
            output.println(", shape = \"box\", style = rounded");
            output.unindent();
            output.println("];");

            for (ControlFlowNode outgoing : node.outgoing) {
                String attrs = colors.containsKey(outgoing) ? "color = " + colors.get(outgoing) : "";
                if (node.immediateDominating.contains(outgoing)) {
                    attrs+= " " + "style = bold";
                }
                output.println("\"" + ("node" + node.index) + "\" -> \"" + ("node" + outgoing.index) + "\" [ " + attrs + "]");
            }
        }

        output.unindent();
        output.println("}");

        return sw.toString();
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\r", "")
                   .replace("\n", "\\l")
                   .replace("|", "\\|")
                   .replace("{", "\\{")
                   .replace("}", "\\}")
                   .replace("<", "\\<")
                   .replace(">", "\\>")
                   .replace("\"", "\\\"");
    }
}
