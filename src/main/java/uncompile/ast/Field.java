package uncompile.ast;

import uncompile.metadata.AccessLevel;
import uncompile.metadata.ClassDescription;
import uncompile.metadata.FieldDescription;
import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

public class Field extends AstNode implements FieldDescription {
    public String name;
    public Class owner;
    public TypeNode type;
    public AccessLevel accessLevel;
    public boolean isStatic;
    public boolean isFinal;
    public boolean isVolatile;
    public boolean isTransient;
    public boolean isSynthetic;
    public Expression initialValue = null;

    public Field(String name, Class owner, TypeNode type, AccessLevel accessLevel, boolean isStatic, boolean isFinal, boolean isVolatile, boolean isTransient, boolean isSynthetic) {
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

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ClassDescription getDeclaringClass() {
        return owner;
    }

    @Override
    public Type getType() {
        return type.toType();
    }

    @Override
    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    @Override
    public boolean isFinal() {
        return isFinal;
    }

    @Override
    public boolean isVolatile() {
        return isVolatile;
    }

    @Override
    public boolean isTransient() {
        return isTransient;
    }

    @Override
    public boolean isSynthetic() {
        return isSynthetic;
    }

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
        w.append(type)
         .append(" ");

        // Name
        w.append(name);

        // Initial value
        if (initialValue != null) {
            w.append(" = ");
            initialValue.append(w);
        }

        w.print(";");
    }
}
