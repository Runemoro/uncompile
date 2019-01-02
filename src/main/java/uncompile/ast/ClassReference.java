package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class ClassReference extends ObjectType implements ClassReferenceParent {
    public ClassReferenceParent parent;
    public String className;
    public boolean isQualified = true;

    public ClassReference(ClassReferenceParent parent, String className) {
        this.parent = parent;
        this.className = className;
    }

    public ClassReference(ClassType ownerType) {
        this(null, null);
        int lastDot = ownerType.fullName.lastIndexOf('.');
        if (lastDot == -1) {
            parent = null;
            className = ownerType.fullName;
        } else {
            parent = new PackageReference(ownerType.fullName.substring(0, lastDot));
            className = ownerType.fullName.substring(lastDot + 1);
        }
    }

    public String getFullName() {
        return parent == null ? className : parent + "." + className;
    }

    @Override
    public ClassType getRawType() {
        return new ClassType(getFullName());
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        if (isQualified && parent != null) {
            w.append((AstNode) parent)
             .append(".");
        }

        w.append(className);
    }
}
