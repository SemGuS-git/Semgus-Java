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
        } else if (idDtoRaw instanceof JSONObject idDto) {
            // it's possibly a (Seq Int) or other types defined in a similar way
            Object nameRaw = idDto.get("kind");
            if (!(nameRaw instanceof String name)) {
                throw new DeserializationException("Identifier name must be a string!", 0);
            }
            Object paramsRaw = idDto.get("params");
            if (!(paramsRaw instanceof JSONArray params)) {
                throw new DeserializationException("'params' is not a JSON array!", 1);
            }
            Index[] indices = new Index[params.size()];
            for (int i = 0; i < indices.length; i++) {
                try {
                    indices[i] = Index.deserialize(params.get(i));
                } catch (DeserializationException e) {
                    throw e.prepend(i + 1);
                }
            }

            return new Identifier(name, indices);
        }
        throw new DeserializationException("Identifier must either be a string, an array or an object containing keys 'kind' and 'params'!");
    }

    @Override
    public String toString() {
        if (indices.length == 0) {
            return name;
        }
        StringBuilder sb = new StringBuilder("(_ ").append(name);
        for (Index index : indices) {
            sb.append(" ").append(index.toString());
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
            if (indexDtoRaw instanceof Long index) {
                return new NInt(index.intValue());
            }
            throw new DeserializationException("Identifier index must be an integer constant!");
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
