package uncompile;

/**
 * Exception throws when attempting decompiling bytecode which would not
 * pass bytecode verification and for which no equivalent Java source code
 * exists.
 */
public class DecompilationNotPossibleException extends RuntimeException {
    public DecompilationNotPossibleException(String message) {
        super(message);
    }
}
