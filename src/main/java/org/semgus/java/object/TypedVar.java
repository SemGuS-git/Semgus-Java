package org.semgus.java.object;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record TypedVar(String name, String type) {

    public static List<TypedVar> fromNamesAndTypes(List<String> names, List<String> types) {
        return IntStream.range(0, names.size())
                .mapToObj(i -> new TypedVar(names.get(i), types.get(i)))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return String.format("%s: %s", name, type);
    }

    public String toStringSExpr() {
        return String.format("(%s %s)", name, type);
    }

}
