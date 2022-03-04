package org.semgus.java.util;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.annotation.Nullable;
import java.util.List;

public class JsonUtils {

    public static Object get(JSONObject obj, String key) throws DeserializationException {
        Object value = obj.get(key);
        if (value == null) {
            throw new DeserializationException("Expected a value to be present!", key);
        }
        return value;
    }

    public static String getString(JSONObject obj, String key) throws DeserializationException {
        Object value = obj.get(key);
        if (!(value instanceof String)) {
            throw getTypeError(key, "a string", value);
        }
        return (String)value;
    }

    public static int getInt(JSONObject obj, String key) throws DeserializationException {
        Object value = obj.get(key);
        if (!(value instanceof Integer)) {
            throw getTypeError(key, "an integer", value);
        }
        return (int)value;
    }

    public static double getNumber(JSONObject obj, String key) throws DeserializationException {
        Object value = obj.get(key);
        if (!(value instanceof Number)) {
            throw getTypeError(key, "a number", value);
        }
        return ((Number)value).doubleValue();
    }

    public static boolean getBoolean(JSONObject obj, String key) throws DeserializationException {
        Object value = obj.get(key);
        if (!(value instanceof Boolean)) {
            throw getTypeError(key, "a boolean", value);
        }
        return (boolean)value;
    }

    public static JSONObject getObject(JSONObject obj, String key) throws DeserializationException {
        Object value = obj.get(key);
        if (!(value instanceof JSONObject)) {
            throw getTypeError(key, "an object", value);
        }
        return (JSONObject)value;
    }

    public static JSONArray getArray(JSONObject obj, String key) throws DeserializationException {
        Object value = obj.get(key);
        if (!(value instanceof JSONArray)) {
            throw getTypeError(key, "an array", value);
        }
        return (JSONArray)value;
    }

    @SuppressWarnings("unchecked")
    public static List<String> ensureStrings(JSONArray arr) throws DeserializationException {
        for (int i = 0; i < arr.size(); i++) {
            if (!(arr.get(i) instanceof String)) {
                throw getTypeError(i, "a string", arr.get(i));
            }
        }
        return (List<String>)arr;
    }

    public static List<String> getStrings(JSONObject obj, String key) throws DeserializationException {
        JSONArray arr = getArray(obj, key);
        try {
            return ensureStrings(arr);
        } catch (DeserializationException e) {
            throw e.prepend(key);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<JSONObject> ensureObjects(JSONArray arr) throws DeserializationException {
        for (int i = 0; i < arr.size(); i++) {
            if (!(arr.get(i) instanceof JSONObject)) {
                throw getTypeError(i, "an object", arr.get(i));
            }
        }
        return (List<JSONObject>)arr;
    }

    public static List<JSONObject> getObjects(JSONObject obj, String key) throws DeserializationException {
        JSONArray arr = getArray(obj, key);
        try {
            return ensureObjects(arr);
        } catch (DeserializationException e) {
            throw e.prepend(key);
        }
    }

    private static DeserializationException getTypeError(String key, String expectedType, @Nullable Object actual) {
        if (actual == null) {
            return new DeserializationException(String.format("Expected %s to be present", expectedType), key);
        }
        return new DeserializationException(
                String.format("Expected %s but got %s", expectedType, actual.getClass().getSimpleName()), key);
    }

    private static DeserializationException getTypeError(int index, String expectedType, @Nullable Object actual) {
        return getTypeError(Integer.toString(index), expectedType, actual);
    }

}
