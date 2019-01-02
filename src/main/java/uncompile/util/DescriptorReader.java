package uncompile.util;

import uncompile.ast.*;

public class DescriptorReader { // TODO: use ASM's SignatureReader instead with conversion to AST types
    public String descriptor;
    public int pos;

    public DescriptorReader(String descriptor, int pos) {
        this.descriptor = descriptor;
        this.pos = pos;
    }

    public Type read() {
        Type type;

        int arrayOrder = 0;
        while (descriptor.charAt(pos) == '[') {
            arrayOrder++;
            pos++;
        }

        switch (descriptor.charAt(pos++)) {
            case 'Z': {
                type = PrimitiveType.BOOLEAN;
                break;
            }

            case 'B': {
                type = PrimitiveType.BYTE;
                break;
            }

            case 'S': {
                type = PrimitiveType.SHORT;
                break;
            }

            case 'I': {
                type = PrimitiveType.INT;
                break;
            }

            case 'J': {
                type = PrimitiveType.LONG;
                break;
            }

            case 'F': {
                type = PrimitiveType.FLOAT;
                break;
            }

            case 'D': {
                type = PrimitiveType.DOUBLE;
                break;
            }

            case 'C': {
                type = PrimitiveType.CHAR;
                break;
            }

            case 'V': {
                type = PrimitiveType.VOID;
                break;
            }

            case 'L': {
                StringBuilder name = new StringBuilder();
                while (descriptor.charAt(pos) != ';') {
                    name.append(descriptor.charAt(pos++));
                }
                pos++;

                type = new ClassReference(new ClassType(name.toString().replace('/', '.')));
                break;
            }

            default: {
                throw new IllegalStateException("Bad descriptor: " + descriptor);
            }
        }

        while (arrayOrder-- > 0) {
            type = new ArrayType(type);
        }

        return type;
    }
}
