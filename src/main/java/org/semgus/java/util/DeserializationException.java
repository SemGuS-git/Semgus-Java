package org.semgus.java.util;

/**
 * An exception indicating that something failed during JSON deserialization. The full path to the error should be
 * accumulated by repeatedly prepending sections of the path in try-catch blocks through {@link #prepend(String)}. See
 * {@link JsonUtils} for examples of this.
 */
public class DeserializationException extends Exception {

    /**
     * The path in the JSON document to the location of the error, delimited by periods.
     */
    private String path;

    /**
     * Constructs a deserialization exception at a given path.
     *
     * @param message An error message indicating what went wrong.
     * @param path    The path to the location of the error.
     */
    public DeserializationException(String message, String path) {
        super(message);
        this.path = path;
    }

    /**
     * Constructs a deserialization exception at a given index.
     *
     * @param message   An error message indicating what went wrong.
     * @param pathIndex The index of the location of the error.
     */
    public DeserializationException(String message, int pathIndex) {
        this(message, Integer.toString(pathIndex));
    }

    /**
     * Constructs a deserialization exception at the root.
     *
     * @param message An error message indicating what went wrong.
     */
    public DeserializationException(String message) {
        this(message, "");
    }

    /**
     * Constructs a deserialization exception at a given path.
     *
     * @param message An error message indicating what went wrong.
     * @param path    The path to the location of the error.
     * @param cause   The exception causing the deserialization error.
     */
    public DeserializationException(String message, String path, Throwable cause) {
        super(message, cause);
        this.path = path;
    }

    /**
     * Constructs a deserialization exception at a given index.
     *
     * @param message   An error message indicating what went wrong.
     * @param pathIndex The index of the location of the error.
     * @param cause     The exception causing the deserialization error.
     */
    public DeserializationException(String message, int pathIndex, Throwable cause) {
        this(message, Integer.toString(pathIndex), cause);
    }

    /**
     * Constructs a deserialization exception at the root.
     *
     * @param message An error message indicating what went wrong.
     * @param cause   The exception causing the deserialization error.
     */
    public DeserializationException(String message, Throwable cause) {
        this(message, "", cause);
    }

    /**
     * Gets the error message describing what went wrong in deserialization.
     *
     * @return The error message.
     */
    public String getErrorMessage() {
        return super.getMessage();
    }

    /**
     * Gets the path to the location of the error in a JSON object or array.
     *
     * @return The path to the error.
     */
    public String getPath() {
        return path;
    }

    @Override
    public String getMessage() {
        return getErrorMessage() + " [" + getPathDisplayText(getPath()) + "]";
    }

    /**
     * Prepends a path segment to the path of this exception.
     *
     * @param pathPrefix The path segment to prepend.
     * @return The augmented exception.
     */
    public DeserializationException prepend(String pathPrefix) {
        path = path.isEmpty() ? pathPrefix : (pathPrefix + "." + path);
        return this;
    }

    /**
     * Prepends an index to the path of this exception.
     *
     * @param pathPrefixIndex The index to prepend.
     * @return The augmented exception.
     */
    public DeserializationException prepend(int pathPrefixIndex) {
        return prepend(Integer.toString(pathPrefixIndex));
    }

    /**
     * Converts a path to a human-readable string. In particular, just converts the root to the string "&lt;root&gt;".
     *
     * @param path The path to stringify.
     * @return A human-readable representation of {@code path}.
     */
    private static String getPathDisplayText(String path) {
        return path.isEmpty() ? "<root>" : path;
    }

}
