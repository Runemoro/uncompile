package uncompile.ast;

import uncompile.metadata.*;
import uncompile.util.IndentingPrintWriter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Method extends AstNode implements MethodDescription {
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
    public TypeNode returnType;
    public List<VariableDeclaration> parameters = new ArrayList<>();
    public List<ReferenceTypeNode> exceptions = new ArrayList<>();
    @Nullable public Block body;

    public Method(String name, Class owner, AccessLevel accessLevel, boolean isStatic, boolean isFinal, boolean isAbstract, boolean isSynchronized, boolean isNative, boolean isSynthetic, boolean isBridge, TypeNode returnType, @Nullable Block body) {
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
    public ClassDescription getDeclaringClass() {
        return owner;
    }

    @Override
    public String getName() {
        return name;
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
    public boolean isAbstract() {
        return isAbstract;
    }

    @Override
    public boolean isSynchronized() {
        return isSynchronized;
    }

    @Override
    public boolean isNative() {
        return isNative;
    }

    @Override
    public boolean isBridge() {
        return isBridge;
    }

    @Override
    public boolean isSynthetic() {
        return isSynthetic;
    }

    @Override
    public List<ReferenceType> getExceptions() {
        return exceptions
                .stream()
                .map(ReferenceTypeNode::toType)
                .collect(Collectors.toList());
    }

    @Override
    public List<TypeParameterType> getTypeParameters() {
        return typeParameters
                .stream()
                .map(TypeParameter::toType)
                .collect(Collectors.toList());
    }

    @Override
    public List<Type> getParameterTypes() {
        return parameters
                .stream()
                .map(p -> p.type.toType())
                .collect(Collectors.toList());
    }

    @Override
    public Type getReturnType() {
        return returnType.toType();
    }

    @Override
    public boolean isStatic() {
        return isStatic;
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

        if (!owner.isInterface() && isAbstract) {
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
            for (TypeNode exception : exceptions) {
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
