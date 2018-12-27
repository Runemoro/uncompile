package uncompile.transformation;

import uncompile.ast.Class;

public interface Transformation {
    void run(Class clazz);
}
