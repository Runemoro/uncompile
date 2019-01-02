package uncompile.metadata;

import java.util.List;

public interface MethodDescription extends MethodType {
    AccessLevel getAccessLevel();

    boolean isFinal();

    boolean isAbstract();

    boolean isSynchronized();

    boolean isNative();

    boolean isBridge();

    boolean isSynthetic();

    List<? extends ReferenceType> getExceptions();
}
