package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

import java.util.ArrayList;
import java.util.List;

public class Method extends AstNode {
    public String name;
    public Class owner;
    public AccessLevel accessLevel;
    public boolean isStatic;
    public boolean isFinal;
    public boolean isAbstract;
    public boolean isSynchronized;
    public boolean isNative;
    public boolean isBridge;
    public boolean isSynthetic;
    public List<TypeParameter> typeParameters = new ArrayList<>();
    public Type returnType;
    public List<VariableDeclaration> parameters = new ArrayList<>();
    public List<Type> exceptions = new ArrayList<>();
    public Block body;

    public Method(String name, Class owner, AccessLevel accessLevel, boolean isStatic, boolean isFinal, boolean isAbstract, boolean isSynchronized, boolean isNative, boolean isSynthetic, boolean isBridge, Type returnType, Block body) {
        this.name = name;
        this.owner = owner;
        this.accessLevel = accessLevel;
        this.isStatic = isStatic;
        this.isFinal = isFinal;
        this.isAbstract = isAbstract;
        this.isSynchronized = isSynchronized;
        this.isNative = isNative;
        this.isSynthetic = isSynthetic;
        this.isBridge = isBridge;
        this.returnType = returnType;
        this.body = body;
    }

    public boolean isClassInitializer() {
        return name.equals("<clinit>");
    }

    public boolean isConstructor() {
        return name.equals("<init>");
    }

    public boolean isSpecial() {
        return isClassInitializer() || isConstructor();
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        if (isClassInitializer()) {
            w.append("static ");
            if (body == null) {
                w.append("{ /* body missing */ }");
            } else {
                body.append(w);
            }
            return;
        }

        // Modifiers
        if (accessLevel != AccessLevel.PROTECTED && !(owner.isInterface() && accessLevel == AccessLevel.PUBLIC)) {
            w.append(accessLevel.toString()).append(" ");
        }

        if (isStatic) {
            w.append("static ");
        }

        if (isFinal) {
            w.append("final ");
        }

        if (isAbstract) {
            w.append("abstract ");
        }

        if (isSynchronized) {
            w.append("synchronized ");
        }

        if (isNative) {
            w.append("native ");
        }

        if (isBridge) {
            w.append("/* bridge */ ");
        }

        if (isSynthetic) {
            w.append("/* synthetic */ ");
        }

        // Type parameters
        if (!typeParameters.isEmpty()) {
            w.append("<");

            boolean first = true;
            for (TypeParameter typeParameter : typeParameters) {
                if (!first) {
                    w.append(", ");
                }
                first = false;

                typeParameter.append(w);
            }

            w.append("> ");
        }

        // Return type
        if (!isConstructor()) {
            returnType.append(w);
            w.append(" ");
        }

        // Name
        w.append(isConstructor() ? owner.name : name);

        // Parameters
        w.append("(");
        boolean first = true;
        for (VariableDeclaration parameter : parameters) {
            if (!first) {
                w.append(", ");
            }
            first = false;

            parameter.append(w);
        }
        w.append(")");

        // Exceptions thrown
        if (!exceptions.isEmpty()) {
            w.append(" throws ");
            first = true;
            for (Type exception : exceptions) {
                if (!first) {
                    w.append(", ");
                }
                first = false;

                exception.append(w);
            }
        }

        // Body
        if (body == null) {
            w.append(";");
        } else {
            w.append(" ");
            body.append(w);
        }
    }
}
