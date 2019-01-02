package uncompile.transformation;

import uncompile.ast.AstVisitor;
import uncompile.ast.Class;
import uncompile.ast.ClassReference;
import uncompile.ast.ClassType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Adds imports to the class and replaces fully qualified names with short names.
 * <p>
 * No dependencies, but should be one of the last transformations done to avoid
 * unnecessary imports (since transformations can remove the need for imports
 * by inlining variables).
 */
public class AddImportsTransform implements Transformation { // TODO: check for conflicts with class name or inner class names
    @Override
    public void run(Class clazz) { // TODO: needs to be adjusted for nested classes
        Map<String, Set<String>> possibleImports = new HashMap<>();

        new AstVisitor() {
            @Override
            public void visit(ClassReference classReference) {
                possibleImports.computeIfAbsent(classReference.className, k -> new HashSet<>())
                               .add(classReference.getFullName());
            }
        }.visit(clazz);

        Set<String> importedClasses = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : possibleImports.entrySet()) {
            if (entry.getValue().size() == 1) {
                String fullyQualified = entry.getValue().iterator().next();

                if (!fullyQualified.contains(".")) {
                    continue;
                }

                importedClasses.add(fullyQualified);

                if (!fullyQualified.substring(0, fullyQualified.lastIndexOf('.')).equals("java.lang")) {
                    clazz.imports.add(new ClassType(fullyQualified));
                }
            }
        }

        new AstVisitor() {
            @Override
            public void visit(ClassReference classReference) {
                classReference.isQualified = !importedClasses.contains(classReference.getFullName());
            }
        }.visit(clazz);
    }
}
