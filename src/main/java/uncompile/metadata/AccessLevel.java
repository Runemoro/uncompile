package uncompile.metadata;

public enum AccessLevel {
    PUBLIC("public"),
    PROTECTED("protected"),
    DEFAULT(""),
    PRIVATE("private");

    private final String string;

    AccessLevel(String string) {
        this.string = string;
    }

    public String toString() {
        return string;
    }
}
