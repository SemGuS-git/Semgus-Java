package org.semgus.java.object;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * A variable annotated with attributes. For instance, variables may be annotated with :input or :output.
 *
 * @param name       The name of the variable.
 * @param attributes The attributes associated with the variable.
 */
public record AnnotatedVar(String name, Map<String, AttributeValue> attributes) {

    @Override
    public String toString() {
        if (attributes.isEmpty()) {
            return name;
        }
        return String.format("%s [%s]", name,
                attributes.entrySet().stream()
                        .map(a -> a.getKey() + " = " + a.getValue())
                        .collect(Collectors.joining("; ")));
    }

}
