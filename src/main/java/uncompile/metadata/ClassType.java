package uncompile.metadata;

public class ClassType extends ReferenceType {
    public static final ClassType OBJECT = new ClassType("java.lang.Object");
    public static final ClassType ENUM = new ClassType("java.lang.Enum");
    public static final ClassType STRING = new ClassType("java.lang.String");
    public static final ClassType CLASS = new ClassType("java.lang.Class");

    // Thankfully, Java doesn't let you give a package and a class the same name,
    // so inner classes named the same as top-level classes (after replacing '$'
    // with '.') isn't a problem.
    public final String fullName;

    public ClassType(String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getInternalName() {
        return fullName.replace('.', '/');
    }

    @Override
    public ClassType getRawType() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this ||
               obj instanceof ClassType &&
               fullName.equals(((ClassType) obj).fullName);
    }

    @Override
    public int hashCode() {
        return fullName.hashCode();
    }

    public String toString() {
        return getFullName();
    }
}
