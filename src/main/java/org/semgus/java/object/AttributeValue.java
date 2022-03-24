package org.semgus.java.object;

import org.json.simple.JSONArray;
import org.semgus.java.util.DeserializationException;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the value of an SMT-Lib attribute. Used by SemGuS for various pieces of metadata.
 */
public interface AttributeValue {

    /**
     * Deserializes an attribute value from the SemGuS JSON format.
     *
     * @param attrValDtoRaw A JSON representation of an attribute value.
     * @return The deserialized attribute value.
     * @throws DeserializationException If {@code attrValDtoRaw} is not a valid representation of an attribute value.
     */
    static AttributeValue deserialize(@Nullable Object attrValDtoRaw) throws DeserializationException {
        if (attrValDtoRaw == null) { // it's a nullary attribute
            return new Unit();
        } else if (attrValDtoRaw instanceof String value) { // it's a string value
            return new AString(value);
        } else if (attrValDtoRaw instanceof JSONArray attrValDto) { // it's a list
            List<AttributeValue> entries = new ArrayList<>();
            for (int i = 0; i < attrValDto.size(); i++) {
                try {
                    entries.add(deserialize(attrValDto.get(i))); // recursively deserialize the list elements
                } catch (DeserializationException e) {
                    throw e.prepend(i);
                }
            }
            return new AList(entries);
        }
        throw new DeserializationException(
                String.format("Could not deserialize attribute value \"%s\"", attrValDtoRaw));
    }

    /**
     * The value for a nullary attribute.
     */
    record Unit() implements AttributeValue {
        // NO-OP
    }

    /**
     * A string attribute value.
     *
     * @param value The string value.
     */
    record AString(String value) implements AttributeValue {
        // NO-OP
    }

    /**
     * A list attribute value. Each element is an attribute value itself.
     *
     * @param entries The list entries.
     */
    record AList(List<AttributeValue> entries) implements AttributeValue {
        // NO-OP
    }

}
