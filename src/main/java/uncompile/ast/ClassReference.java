package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class ClassReference extends ObjectType { // TODO: inner class support
    public String packageName;
    public String className;
    public boolean isQualified = true;

    public ClassReference(String packageName, String className) {
        this.packageName = packageName;
        this.className = className;
    }

    public ClassReference(ClassType ownerType) {
        this(ownerType.fullName.substring(0, ownerType.fullName.lastIndexOf('.')), ownerType.fullName.substring(ownerType.fullName.lastIndexOf('.') + 1));
    }

    public String getFullName() {
        return packageName + "." + className;
    }

    @Override
    public ClassType getRawType() {
        return new ClassType(packageName + "." + className);
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        if (isQualified) {
            w.append(packageName)
             .append(".");
        }

        w.append(className);
    }
}
