package org.semgus.java.util;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Helper class for deserializing simple data types from JSON objects and arrays.
 */
public final class JsonUtils {

    /**
     * Empty helper class constructor.
     */
    private JsonUtils() {
        // NO-OP
    }

    /**
     * Gets an arbitrary value from a JSON object.
     *
     * @param obj The JSON object.
     * @param key The key to look up.
     * @return The value associated with {@code key}.
     * @throws DeserializationException If there is no value in {@code obj} at {@code key}.
     */
    public static Object get(JSONObject obj, String key) throws DeserializationException {
        Object value = obj.get(key);
        if (value == null) {
            throw new DeserializationException("Expected a value to be present!", key);
        }
        return value;
    }

    /**
     * Gets a string from a JSON object.
     *
     * @param obj The JSON object.
     * @param key The key to look up.
     * @return The string value associated with {@code key}.
     * @throws DeserializationException If there is no string in {@code obj} at {@code key}.
     */
    public static String getString(JSONObject obj, String key) throws DeserializationException {
        Object value = obj.get(key);
        if (!(value instanceof String)) {
            throw getTypeError(key, "a string", value);
        }
        return (String)value;
    }

    /**
     * Gets a (long) integer from a JSON object.
     *
     * @param obj The JSON object.
     * @param key The key to look up.
     * @return The (long) integer value associated with {@code key}.
     * @throws DeserializationException If there is no integer in {@code obj} at {@code key}.
     */
    public static long getLong(JSONObject obj, String key) throws DeserializationException {
        Object value = obj.get(key);
        if (!(value instanceof Long)) {
            throw getTypeError(key, "an integer", value);
        }
        return (long)value;
    }

    /**
     * Gets a integer from a JSON object.
     *
     * @param obj The JSON object.
     * @param key The key to look up.
     * @return The integer value associated with {@code key}.
     * @throws DeserializationException If there is no integer in {@code obj} at {@code key}, or if the value is too
     *                                  large for the 32-bit signed integer type.
     */
    public static int getInt(JSONObject obj, String key) throws DeserializationException {
        long longVal = getLong(obj, key);
        if (longVal > Integer.MAX_VALUE || longVal < Integer.MIN_VALUE) {
            throw getTypeError(key, "a 32-bit integer", longVal);
        }
        return (int)longVal;
    }

    /**
     * Gets a number from a JSON object.
     *
     * @param obj The JSON object.
     * @param key The key to look up.
     * @return The string value associated with {@code key}.
     * @throws DeserializationException If there is no number in {@code obj} at {@code key}.
     */
    public static double getNumber(JSONObject obj, String key) throws DeserializationException {
        Object value = obj.get(key);
        if (!(value instanceof Number)) {
            throw getTypeError(key, "a number", value);
        }
        return ((Number)value).doubleValue();
    }

    /**
     * Gets a boolean from a JSON object.
     *
     * @param obj The JSON object.
     * @param key The key to look up.
     * @return The boolean value associated with {@code key}.
     * @throws DeserializationException If there is no boolean in {@code obj} at {@code key}.
     */
    public static boolean getBoolean(JSONObject obj, String key) throws DeserializationException {
        Object value = obj.get(key);
        if (!(value instanceof Boolean)) {
            throw getTypeError(key, "a boolean", value);
        }
        return (boolean)value;
    }

    /**
     * Gets a child JSON object from a JSON object.
     *
     * @param obj The parent JSON object.
     * @param key The key to look up.
     * @return The child JSON object associated with {@code key}.
     * @throws DeserializationException If there is no JSON object in {@code obj} at {@code key}.
     */
    public static JSONObject getObject(JSONObject obj, String key) throws DeserializationException {
        Object value = obj.get(key);
        if (!(value instanceof JSONObject)) {
            throw getTypeError(key, "an object", value);
        }
        return (JSONObject)value;
    }

    /**
     * Gets a JSON array from a JSON object.
     *
     * @param obj The JSON object.
     * @param key The key to look up.
     * @return The JSON array associated with {@code key}.
     * @throws DeserializationException If there is no JSON array in {@code obj} at {@code key}.
     */
    public static JSONArray getArray(JSONObject obj, String key) throws DeserializationException {
        Object value = obj.get(key);
        if (!(value instanceof JSONArray)) {
            throw getTypeError(key, "an array", value);
        }
        return (JSONArray)value;
    }

    /**
     * Asserts that a JSON array contains only strings.
     *
     * @param arr The JSON array.
     * @return The array coerced to a list of strings.
     * @throws DeserializationException If {@code arr} is not an array of strings.
     */
    @SuppressWarnings("unchecked")
    public static List<String> ensureStrings(JSONArray arr) throws DeserializationException {
        for (int i = 0; i < arr.size(); i++) {
            if (!(arr.get(i) instanceof String)) {
                throw getTypeError(i, "a string", arr.get(i));
            }
        }
        return (List<String>)arr;
    }

    /**
     * Gets an array of strings from a JSON object.
     *
     * @param obj The JSON object.
     * @param key The key to look up.
     * @return The array of strings associated with {@code key}.
     * @throws DeserializationException If there is no array of strings in {@code obj} at {@code key}.
     */
    public static List<String> getStrings(JSONObject obj, String key) throws DeserializationException {
        JSONArray arr = getArray(obj, key);
        try {
            return ensureStrings(arr);
        } catch (DeserializationException e) {
            throw e.prepend(key);
        }
    }

    /**
     * Asserts that a JSON array contains only JSON objects.
     *
     * @param arr The JSON array.
     * @return The array coerced to a list of JSON objects.
     * @throws DeserializationException If {@code arr} is not an array of JSON objects.
     */
    @SuppressWarnings("unchecked")
    public static List<JSONObject> ensureObjects(JSONArray arr) throws DeserializationException {
        for (int i = 0; i < arr.size(); i++) {
            if (!(arr.get(i) instanceof JSONObject)) {
                throw getTypeError(i, "an object", arr.get(i));
            }
        }
        return (List<JSONObject>)arr;
    }

    /**
     * Gets an array of JSON objects from a JSON object.
     *
     * @param obj The JSON object.
     * @param key The key to look up.
     * @return The array of JSON objects associated with {@code key}.
     * @throws DeserializationException If there is no array of JSON objects in {@code obj} at {@code key}.
     */
    public static List<JSONObject> getObjects(JSONObject obj, String key) throws DeserializationException {
        JSONArray arr = getArray(obj, key);
        try {
            return ensureObjects(arr);
        } catch (DeserializationException e) {
            throw e.prepend(key);
        }
    }

    /**
     * Constructs an exception stating that there was a type mismatch while deserializing an element of a JSON object.
     *
     * @param key          The key whose value has an unexpected type.
     * @param expectedType A human-readable name for the expected type.
     * @param actual       The actual object.
     * @return A new deserialization exception.
     */
    private static DeserializationException getTypeError(String key, String expectedType, @Nullable Object actual) {
        if (actual == null) {
            return new DeserializationException(String.format("Expected %s to be present", expectedType), key);
        }
        return new DeserializationException(
                String.format("Expected %s but got %s", expectedType, actual.getClass().getSimpleName()), key);
    }

    /**
     * Constructs an exception stating that there was a type mismatch while deserializing an element of a JSON array.
     *
     * @param index        The index whose value has an unexpected type.
     * @param expectedType A human-readable name for the expected type.
     * @param actual       The actual object.
     * @return A new deserialization exception.
     */
    private static DeserializationException getTypeError(int index, String expectedType, @Nullable Object actual) {
        return getTypeError(Integer.toString(index), expectedType, actual);
    }

}
