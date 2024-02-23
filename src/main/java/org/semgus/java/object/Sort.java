package org.semgus.java.object;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.semgus.java.util.DeserializationException;
import org.semgus.java.util.JsonUtils;

import java.util.Arrays;
import java.util.List;
import org.semgus.java.object.Identifier;

/**
 * Represents a sort as per the SMTLIB spec. Is either an identifier, or an identifier taking sorts as arguments
 * (i.e., a type without parameters or a type with parameters specified) 
 * 
 * @param base    The identifier itself.
 * @param sorts   The optional parameters to the sort.
 */
public record Sort(Identifier ident, Sort... params) {
    /**
     * Deserializes an identifier which may or may not be indexed.
     *
     * @param idDtoRaw A JSON value representing an optionally-indexed identifier.
     * @return The deserialized identifier.
     * @throws DeserializationException If {@code idDtoRaw} is not a valid representation of an identifier.
     */
    public static Sort deserialize(Object idDtoRaw) throws DeserializationException {
        if (idDtoRaw instanceof JSONObject idDto) { // if it's an object; then it's a parametrized sort.
            // first kind is name, params are params
            // parse identifier name
            if (!idDto.containsKey("kind") || !idDto.containsKey("params")) {
                throw new DeserializationException("Parametric sort is missing information!");
            }
            Identifier ident = Identifier.deserialize(idDto.get("kind"));

            if (idDto.get("params") instanceof JSONArray idParams) {
                // parse args
                Sort[] params = new Sort[idParams.size()];
                for (int i = 0; i < params.length; i++) {
                    try {
                        params[i] = Sort.deserialize(idParams.get(i));
                    } catch (DeserializationException e) {
                        throw e.prepend(i);
                    }
                }
                return new Sort(ident, params);
            }
            else {
                throw new DeserializationException("Parametric sort's parameters must be specified as a list!");
            }
        }
        else {
            return new Sort(Identifier.deserialize(idDtoRaw));
        }
    }

    /**
     * Deserializes a sort from the SemGuS JSON format at a given key in a parent JSON object.
     *
     * @param parentDto The parent JSON object.
     * @param key       The key whose value should be deserialized.
     * @return The deserialized sort.
     * @throws DeserializationException If the value at {@code key} is not a valid representation of an sort.
     */
    public static Sort deserializeAt(JSONObject parentDto, String key) throws DeserializationException {
        Object identifierDto = JsonUtils.get(parentDto, key);
        try {
            return deserialize(identifierDto);
        } catch (DeserializationException e) {
            throw e.prepend(key);
        }
    }

    /**
     * Deserializes a list of sorts from a JSON array.
     *
     * @param idsDto The JSON array of sorts.
     * @return The list of the deserialized sorts.
     * @throws DeserializationException If {@code idsDto} is not an array of valid representations of sorts.
     */
    public static List<Sort> deserializeList(JSONArray idsDto) throws DeserializationException {
        Sort[] ids = new Sort[idsDto.size()];
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
        if (params.length == 0) {
            return ident.toString();
        }
        StringBuilder sb = new StringBuilder("(").append(ident.toString());
        for (Sort sort : params) {
            sb.append(" ").append(sort.toString());
        }
        return sb.append(")").toString();
    }
}