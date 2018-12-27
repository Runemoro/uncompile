package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class Field extends AstNode {
    public String name;
    public Class owner;
    public Type type;
    public AccessLevel accessLevel;
    public boolean isStatic;
    public boolean isFinal;
    public boolean isVolatile;
    public boolean isTransient;

    public Field(String name, Class owner, Type type, AccessLevel accessLevel, boolean isStatic, boolean isFinal, boolean isVolatile, boolean isTransient, boolean isSynthetic) {
        this.name = name;
        this.owner = owner;
        this.type = type;
        this.accessLevel = accessLevel;
        this.isStatic = isStatic;
        this.isFinal = isFinal;
        this.isVolatile = isVolatile;
        this.isTransient = isTransient;
        this.isSynthetic = isSynthetic;
    }

    public boolean isSynthetic;
    public Expression initialValue = null;

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        // Modifiers
        if (accessLevel != AccessLevel.DEFAULT) {
            w.append(accessLevel.toString()).append(" ");
        }

        if (isStatic) {
            w.append("static ");
        }

        if (isFinal) {
            w.append("final ");
        }

        if (isVolatile) {
            w.append("volatile ");
        }

        if (isTransient) {
            w.append("transient ");
        }

        if (isSynthetic) {
            w.append("/* synthetic */ ");
        }

        // Type
        type.append(w);
        w.append(" ");

        // Name
        w.append(name);

        // Initial value
        if (initialValue != null) {
            w.append(" = ");
            initialValue.append(w);
        }

        w.println(";");
    }
}
