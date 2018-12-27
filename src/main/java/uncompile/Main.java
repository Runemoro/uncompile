package uncompile;

import uncompile.ast.Class;
import uncompile.astbuilder.ClassBuilder;
import uncompile.transformation.AstTransformations;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        ClassBuilder classBuilder = new ClassBuilder(new SimpleClassProvider());
        Class decompiled = classBuilder.getDecompiled("Test");

        AstTransformations.run(decompiled);

        System.out.println(decompiled);
    }

    private static class SimpleClassProvider implements ClassProvider {
        // TODO: switch to guava cache?
        private Map<String, ClassNode> classCache = new HashMap<>();

        @Override
        public ClassNode getClass(String name) {
            return classCache.computeIfAbsent(name, k -> {
                try {
                    String resourceName = name.replace('.', '/') + ".class";
                    File classFile = new File(resourceName);

                    InputStream classResource = classFile.exists() ?
                            new FileInputStream(classFile) :
                            Main.class.getResourceAsStream(resourceName);

                    if (classResource == null) {
                        throw new RuntimeException("class missing: " + name);
                    }

                    ClassNode classNode = new ClassNode();
                    new ClassReader(classResource).accept(classNode, 0);
                    return classNode;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
