package org.semgus.java.object;

import org.json.simple.JSONObject;
import org.semgus.java.util.DeserializationException;
import org.semgus.java.util.JsonUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a relation application to variables.
 *
 * @param name      The name of the relation.
 * @param arguments The variables that are passed as arguments to the relation.
 */
public record RelationApp(String name, List<TypedVar> arguments) {

    /**
     * Deserializes a relation application from the SemGuS JSON format.
     *
     * @param relAppDto JSON object representing the relation application.
     * @return The deserialized relation application.
     * @throws DeserializationException If {@code relAppDto} is not a valid representation of a relation application.
     */
    public static RelationApp deserialize(JSONObject relAppDto) throws DeserializationException {
        String name = JsonUtils.getString(relAppDto, "name");
        List<String> sig = JsonUtils.getStrings(relAppDto, "signature");
        List<String> args = JsonUtils.getStrings(relAppDto, "arguments");
        if (sig.size() != args.size()) {
            throw new DeserializationException(String.format(
                    "Signature and arguments of relation application have different lengths %d != %d",
                    sig.size(), args.size()));
        }
        return new RelationApp(name, TypedVar.fromNamesAndTypes(args, sig));
    }

    @Override
    public String toString() {
        return String.format("%s(%s)",
                name, arguments.stream().map(TypedVar::toString).collect(Collectors.joining(", ")));
    }

}
