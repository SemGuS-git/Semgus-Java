package org.semgus.java.object;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.semgus.java.util.DeserializationException;
import org.semgus.java.util.JsonUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Represents an identifier that may be annotated with additional indices. For example, an identifier referring to a bit
 * vector type may be indexed with an integer representing the length of corresponding bit vectors.
 *
 * @param name    The identifier itself.
 * @param indices The optional indices to the identifier.
 */
public record Identifier(String name, Index... indices) {

    /**
     * Deserializes an identifier which may or may not be indexed.
     *
     * @param idDtoRaw A JSON value representing an optionally-indexed identifier.
     * @return The deserialized identifier.
     * @throws DeserializationException If {@code idDtoRaw} is not a valid representation of an identifier.
     */
    public static Identifier deserialize(Object idDtoRaw) throws DeserializationException {
        if (idDtoRaw instanceof String name) { // just a string; it's non-indexed
            return new Identifier(name);
        } else if (idDtoRaw instanceof JSONArray idDto) { // it's an array; first elem is name, rest are indices
            // parse identifier name
            if (idDto.isEmpty()) {
                throw new DeserializationException("Identifier must include name!");
            }
            Object nameRaw = idDto.get(0);
            if (!(nameRaw instanceof String)) {
                throw new DeserializationException("Identifier name must be a string!", 0);
            }

            // parse indices
            Index[] indices = new Index[idDto.size() - 1];
            for (int i = 0; i < indices.length; i++) {
                try {
                    indices[i] = Index.deserialize(idDto.get(i + 1));
                } catch (DeserializationException e) {
                    throw e.prepend(i + 1);
                }
            }

            return new Identifier((String)nameRaw, indices);
        }
        throw new DeserializationException("Identifier must either be a string or an array!");
    }

    /**
     * Deserializes an identifier from the SemGuS JSON format at a given key in a parent JSON object.
     *
     * @param parentDto The parent JSON object.
     * @param key       The key whose value should be deserialized.
     * @return The deserialized identifier.
     * @throws DeserializationException If the value at {@code key} is not a valid representation of an identifier.
     */
    public static Identifier deserializeAt(JSONObject parentDto, String key) throws DeserializationException {
        Object identifierDto = JsonUtils.get(parentDto, key);
        try {
            return deserialize(identifierDto);
        } catch (DeserializationException e) {
            throw e.prepend(key);
        }
    }

    /**
     * Deserializes a list of identifiers from a JSON array.
     *
     * @param idsDto The JSON array of identifiers.
     * @return The list of the deserialized identifiers.
     * @throws DeserializationException If {@code idsDto} is not an array of valid representations of identifiers.
     */
    public static List<Identifier> deserializeList(JSONArray idsDto) throws DeserializationException {
        Identifier[] ids = new Identifier[idsDto.size()];
        for (int i = 0; i < ids.length; i++) {
            try {
                ids[i] = deserialize(idsDto.get(i));
            } catch (DeserializationException e) {
                throw e.prepend(i);
            }
        }
        return Arrays.asList(ids);
    }

    @Override
    public String toString() {
        if (indices.length == 0) {
            return name;
        }
        StringBuilder sb = new StringBuilder("(").append(name);
        for (Index index : indices) {
            sb.append(" ").append(index);
        }
        return sb.append(")").toString();
    }

    /**
     * An index value for an indexed identifier.
     */
    public sealed interface Index {

        /**
         * Deserializes an index value for an identifier.
         *
         * @param indexDtoRaw A JSON value representing an index value.
         * @return The deserialized index value.
         * @throws DeserializationException If {@code indexDtoRaw} is not a valid representation of an index value.
         */
        static Index deserialize(Object indexDtoRaw) throws DeserializationException {
            if (indexDtoRaw instanceof String index) {
                return new NString(index);
            } else if (indexDtoRaw instanceof Long index) {
                return new NInt(index.intValue());
            }
            throw new DeserializationException("Identifier index must either be a string or integer constant!");
        }

        /**
         * A string index value.
         *
         * @param value The string value.
         */
        record NString(String value) implements Index {

            @Override
            public String toString() {
                return "\"" + value + "\"";
            }

        }

        /**
         * An integer index value.
         *
         * @param value The integer value.
         */
        record NInt(int value) implements Index {

            @Override
            public String toString() {
                return Integer.toString(value);
            }

        }

    }

}
