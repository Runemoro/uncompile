package uncompile.util;

import uncompile.ast.AstNode;

import java.io.PrintWriter;
import java.io.Writer;

public class IndentingPrintWriter extends PrintWriter {
    private boolean needsIndent = true;
    private int indent = 0;

    public IndentingPrintWriter(Writer out) {
        super(out);
    }

    public IndentingPrintWriter indent() {
        indent(4);
        return this;
    }

    public IndentingPrintWriter indent(int i) {
        indent += i;
        return this;
    }

    public IndentingPrintWriter unindent() {
        unindent(4);
        return this;
    }

    public IndentingPrintWriter unindent(int i) {
        indent -= i;
        return this;
    }

    public IndentingPrintWriter append(AstNode node) {
        node.append(this);
        return this;
    }

    @Override
    public void println() {
        super.println();
        needsIndent = true;
    }

    public IndentingPrintWriter append(Object obj) {
        super.append(obj.toString());
        return this;
    }

    @Override
    public IndentingPrintWriter append(CharSequence csq) {
        super.append(csq);
        return this;
    }

    @Override
    public void write(String s) {
        if (needsIndent) {
            for (int i = 0; i < indent; i++) {
                super.write(" ");
            }
        }

        super.write(s);
        needsIndent = false;
    }
}
