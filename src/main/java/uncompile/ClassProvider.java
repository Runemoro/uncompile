package uncompile;

import org.objectweb.asm.tree.ClassNode;

public interface ClassProvider {
    ClassNode getClass(String name);
}
