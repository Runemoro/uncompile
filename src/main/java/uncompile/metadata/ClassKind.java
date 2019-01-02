package uncompile.metadata;

public enum ClassKind {
    CLASS("class"),
    ENUM("enum"),
    INTERFACE("interface"),
    ANNOTATION("annotation");

    private final String name;

    ClassKind(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
