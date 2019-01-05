package uncompile.transformation;

import uncompile.ast.Class;

public class PrintClassForDebugging implements Transformation {
    @Override
    public void run(Class clazz) {
        System.out.println(clazz);
    }
}
