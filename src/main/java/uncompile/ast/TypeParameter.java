package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class TypeParameter extends ObjectType { // TODO: scope
    public String name;
    public ObjectType extendsBound;

    public TypeParameter(String name, ObjectType extendsBound) {
        this.name = name;
        this.extendsBound = extendsBound;
    }

    @Override
    public ClassType getRawType() {
        return extendsBound.getRawType();
    }


    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append(name);

        if (extendsBound != null) {
            w.append(" extends ");
            extendsBound.append(w);
        }
    }
}
