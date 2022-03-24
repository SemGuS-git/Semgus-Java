package org.semgus.java.object;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Represents a variable with an associated type.
 *
 * @param name The variable's name.
 * @param type The name of the variable's type.
 */
public record TypedVar(String name, String type) {

    /**
     * Zips a list of variable names and a list of type names into a list of typed variables. The two lists should have
     * the same length.
     *
     * @param names A list of variable names.
     * @param types A list of type names.
     * @return A new list of typed variables.
     */
    public static List<TypedVar> fromNamesAndTypes(List<String> names, List<String> types) {
        return IntStream.range(0, names.size())
                .mapToObj(i -> new TypedVar(names.get(i), types.get(i)))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return String.format("%s: %s", name, type);
    }

    /**
     * Stringifies this typed variable as an s-expression of the form "(var type)".
     *
     * @return The stringified typed variable.
     */
    public String toStringSExpr() {
        return String.format("(%s %s)", name, type);
    }

}
