package org.semgus.java.object;

import org.json.simple.JSONArray;
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

        // deserialize argument name and type lists
        JSONArray sigDto = JsonUtils.getArray(relAppDto, "signature");
        List<String> args = JsonUtils.getStrings(relAppDto, "arguments");
        if (sigDto.size() != args.size()) {
            throw new DeserializationException(String.format(
                    "Signature and arguments of relation application have different lengths %d != %d",
                    sigDto.size(), args.size()));
        }

        // deserialize type identifiers
        List<Identifier> types;
        try {
            types = Identifier.deserializeList(sigDto);
        } catch (DeserializationException e) {
            throw e.prepend("signature");
        }

        return new RelationApp(name, TypedVar.fromNamesAndTypes(args, types));
    }

    /**
     * Deserializes an relation application from the SemGuS JSON format at a given key in a parent JSON object.
     *
     * @param parentDto The parent JSON object.
     * @param key       The key whose value should be deserialized.
     * @return The deserialized relation application.
     * @throws DeserializationException If the value at {@code key} is not a valid representation of a relation
     *                                  application.
     */
    public static RelationApp deserializeAt(JSONObject parentDto, String key) throws DeserializationException {
        JSONObject relAppDto = JsonUtils.getObject(parentDto, key);
        try {
            return deserialize(relAppDto);
        } catch (DeserializationException e) {
            throw e.prepend(key);
        }
    }

    @Override
    public String toString() {
        return String.format("%s(%s)",
                name, arguments.stream().map(TypedVar::toString).collect(Collectors.joining(", ")));
    }

}
