package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

import java.io.IOException;
import java.io.StringWriter;

public abstract class AstNode { // TODO: use builders for everything, use print visitor rather than print methods
    public String toString() {
        try (StringWriter stringWriter = new StringWriter();
             IndentingPrintWriter w = new IndentingPrintWriter(stringWriter)) {
            append(w);
            return stringWriter.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract void accept(AstVisitor visitor);

    public abstract void append(IndentingPrintWriter w);

    @Override
    public final boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public final int hashCode() {
        return super.hashCode();
    }
}
