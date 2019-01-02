package uncompile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import uncompile.ast.Class;
import uncompile.astbuilder.ClassBuilder;
import uncompile.transformation.AstTransformations;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        ClassProvider classProvider = new SimpleClassProvider();

        ClassBuilder classBuilder = new ClassBuilder(classProvider);
        new ClassReader(classProvider.getClass("Test")).accept(classBuilder, ClassReader.EXPAND_FRAMES);
        Class decompiled = classBuilder.getResult();

        AstTransformations.run(decompiled);

        System.out.println(decompiled);
    }

    private static class SimpleClassProvider implements ClassProvider {
        // TODO: switch to guava cache?
        private Map<String, Optional<byte[]>> classCache = new HashMap<>();

        @Override
        public byte[] getClass(String name) {
            return classCache.computeIfAbsent(name, k -> {
                try {
                    String resourceName = name.replace('.', '/') + ".class";
                    File classFile = new File(resourceName);

                    InputStream classResource = classFile.exists() ?
                            new FileInputStream(classFile) :
                            Main.class.getResourceAsStream(resourceName);

                    if (classResource == null) {
                        return Optional.empty();
                    }

                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    int nRead;
                    byte[] data = new byte[16384];
                    while ((nRead = classResource.read(data, 0, data.length)) != -1) {
                        bytes.write(data, 0, nRead);
                    }
                    return Optional.of(bytes.toByteArray());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).orElse(null);
        }
    }
}
