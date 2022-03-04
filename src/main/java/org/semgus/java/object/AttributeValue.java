package org.semgus.java.object;

import org.json.simple.JSONArray;
import org.semgus.java.util.DeserializationException;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public interface AttributeValue {

    static AttributeValue deserialize(@Nullable Object attrValDtoRaw) throws DeserializationException {
        if (attrValDtoRaw == null) {
            return new Unit();
        } else if (attrValDtoRaw instanceof String value) {
            return new AString(value);
        } else if (attrValDtoRaw instanceof JSONArray attrValDto) {
            List<AttributeValue> entries = new ArrayList<>();
            for (int i = 0; i < attrValDto.size(); i++) {
                try {
                    entries.add(deserialize(attrValDto.get(i)));
                } catch (DeserializationException e) {
                    throw e.prepend(i);
                }
            }
            return new AList(entries);
        }
        throw new DeserializationException(
                String.format("Could not deserialize attribute value \"%s\"", attrValDtoRaw));
    }

    record Unit() implements AttributeValue {
        // NO-OP
    }

    record AString(String value) implements AttributeValue {
        // NO-OP
    }

    record AList(List<AttributeValue> entries) implements AttributeValue {
        // NO-OP
    }

}
