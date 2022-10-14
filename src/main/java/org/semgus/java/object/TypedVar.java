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
public record TypedVar(String name, Identifier type) {

    /**
     * Zips a list of variable names and a list of type identifiers into a list of typed variables. The two lists should
     * have the same length.
     *
     * @param names A list of variable names.
     * @param types A list of type identifiers.
     * @return A new list of typed variables.
     */
    public static List<TypedVar> fromNamesAndTypes(List<String> names, List<Identifier> types) {
        return IntStream.range(0, names.size())
                .mapToObj(i -> new TypedVar(names.get(i), types.get(i)))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return String.format("(%s %s)", name, type);
    }

    /**
     * Use {@link #toString()} instead.
     *
     * @return The stringified typed variable.
     */
    @Deprecated(since = "1.1.0")
    public String toStringSExpr() {
        return toString();
    }

}
