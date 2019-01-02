package uncompile.astbuilder;

import uncompile.metadata.ClassDescription;
import uncompile.metadata.FieldDescription;
import uncompile.metadata.MethodDescription;
import uncompile.metadata.Type;
import uncompile.util.DescriptorReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class DescriptionProvider {
    private Map<String, ClassDescription> classes = new HashMap<>();
    private Map<String, MethodDescription> methods = new HashMap<>();
    private Map<String, FieldDescription> fields = new HashMap<>();

    public void addClassDescription(String name, ClassDescription description) {
        classes.put(name, description);
    }

    public void addMethodDescription(String owner, String name, String descriptor, MethodDescription description) {
        methods.put(owner + ":" + name + descriptor, description);
    }

    public void addFieldDescription(String owner, String name, String descriptor, FieldDescription fieldDescription) {
        fields.put(owner + ":" + name + descriptor, fieldDescription);
    }

    public ClassDescription getClassDescription(String name) {
        return classes.computeIfAbsent(name, k -> {
            ClassDescription classDescription = createClassDescription();
            return classDescription != null ? classDescription : new UnresolvedClassDescription(name.replace('/', '.'));
        });
    }

    protected abstract ClassDescription createClassDescription();

    public FieldDescription getFieldDescription(String owner, String name, String descriptor, boolean isStatic) {
        return fields.computeIfAbsent(owner + ":" + name + descriptor, k -> {
            DescriptorReader r = new DescriptorReader(descriptor, 0);
            Type type = r.read();

            ClassDescription classDescription = getClassDescription(owner);
            FieldDescription result = new UnresolvedFieldDescription(classDescription, name, type, isStatic);
            if (classDescription instanceof UnresolvedClassDescription) {
                ((UnresolvedClassDescription) classDescription).getFields().add(result);
            }
            return result;
        });
    }

    public MethodDescription getMethodDescription(String owner, String name, String descriptor, boolean isStatic) {
        return methods.computeIfAbsent(owner + ":" + name + descriptor, k -> {
            DescriptorReader r = new DescriptorReader(descriptor, 1);
            List<Type> parameterTypes = new ArrayList<>();
            while (r.descriptor.charAt(r.pos) != ')') {
                parameterTypes.add(r.read());
            }
            r.pos++;
            Type returnType = r.read();

            ClassDescription classDescription = getClassDescription(owner);
            MethodDescription result = new UnresolvedMethodDescription(classDescription, name, parameterTypes, returnType, isStatic);
            if (classDescription instanceof UnresolvedClassDescription) {
                ((UnresolvedClassDescription) classDescription).getMethods().add(result);
            }
            return result;
        });
    }
}
