package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class PackageReference extends AstNode implements ClassReferenceParent {
    public String packageName;

    public PackageReference(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append(packageName);
    }
}
