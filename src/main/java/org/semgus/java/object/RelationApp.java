package org.semgus.java.object;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.semgus.java.util.DeserializationException;
import org.semgus.java.util.JsonUtils;

import java.util.Arrays;
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

        // deserialize argument name and type lists
        JSONArray sigDto = JsonUtils.getArray(relAppDto, "signature");
        List<String> args = JsonUtils.getStrings(relAppDto, "arguments");
        if (sigDto.size() != args.size()) {
            throw new DeserializationException(String.format(
                    "Signature and arguments of relation application have different lengths %d != %d",
                    sigDto.size(), args.size()));
        }

        // deserialize type identifiers
        Identifier[] types = new Identifier[sigDto.size()];
        for (int i = 0; i < types.length; i++) {
            try {
                types[i] = Identifier.deserialize(sigDto.get(i));
            } catch (DeserializationException e) {
                throw e.prepend("signature." + i);
            }
        }

        return new RelationApp(name, TypedVar.fromNamesAndTypes(args, Arrays.asList(types)));
    }

    @Override
    public String toString() {
        return String.format("%s(%s)",
                name, arguments.stream().map(TypedVar::toString).collect(Collectors.joining(", ")));
    }

}
