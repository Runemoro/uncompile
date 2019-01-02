package uncompile.util;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class SpecialVertexGroupFinder {
    public interface Vertex<V> {
        Collection<V> getIncomingVertices();

        Collection<V> getOutgoingVertices();
    }

    /**
     * Given a graph, finds a strict subset S of its vertices such that:
     * - S contains at least two elements
     * - There is at most vertex not in S with edges to vertices in S
     * - There is at most vertex in S with edges to vertices not in S
     *
     * @param graph the set of all vertices in the graph
     * @param <V>   the type of the vertices
     * @return any subset of the vertices with the property described above,
     * or null if none exists
     */
    public static <V extends Vertex<V>> Set<V> getSpecialVertexGroup(Set<V> graph) {
        for (V start : graph) {
            for (V end : graph) {
                // Suppose that 'start' is in S and 'end' is not in S, and that all edges
                // from outside S to inside S start at 'start', and all edges from inside
                // S to outside S start at 'end'.
                //
                // If removing all edges starting at start and at end splits the graph into
                // more than one connected component, and the connected component 'start'
                // belongs to is not the whole graph, then that connected component has the
                // property we want. Otherwise, we need to try a new 'start' and 'end'.

                // Order doesn't actually matter, but we want the resulting group to
                // be deterministic to make debugging easier, so use LinkedHashSet
                Set<V> group = new LinkedHashSet<>();
                Set<V> pending = new LinkedHashSet<>();
                pending.add(start);
                while (!pending.isEmpty()) {
                    V vertex = pending.iterator().next();
                    pending.remove(vertex);

                    group.add(vertex);
                    if (vertex != start) {
                        for (V incoming : vertex.getIncomingVertices()) {
                            if (!group.contains(incoming)) {
                                pending.add(incoming);
                            }
                        }
                    }

                    for (V outgoing : vertex.getOutgoingVertices()) {
                        if (outgoing != end) {
                            if (!group.contains(outgoing)) {
                                pending.add(outgoing);
                            }
                        }
                    }
                }

                if (group.size() > 1 && group.size() < graph.size()) {
                    return group;
                }
            }
        }

        return null;
    }
}
