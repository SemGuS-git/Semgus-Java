package org.semgus.java.util;

public class DeserializationException extends Exception {

    private final String errorMessage;
    private final String path;

    public DeserializationException(String message, String path) {
        super(message + ": " + getPathDisplayText(path));
        this.errorMessage = message;
        this.path = path;
    }

    public DeserializationException(String message, int pathIndex) {
        this(message, Integer.toString(pathIndex));
    }

    public DeserializationException(String message) {
        this(message, "");
    }

    public DeserializationException(String message, String path, Throwable cause) {
        super(message + ": " + getPathDisplayText(path), cause);
        this.errorMessage = message;
        this.path = path;
    }

    public DeserializationException(String message, int pathIndex, Throwable cause) {
        this(message, Integer.toString(pathIndex), cause);
    }

    public DeserializationException(String message, Throwable cause) {
        this(message, "", cause);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getPath() {
        return path;
    }

    public DeserializationException prepend(String pathPrefix) {
        return pathPrefix.isEmpty() ? this
                : new DeserializationException(errorMessage, pathPrefix + "." + path, getCause());
    }

    public DeserializationException prepend(int pathPrefixIndex) {
        return prepend(Integer.toString(pathPrefixIndex));
    }

    private static String getPathDisplayText(String path) {
        return path.isEmpty() ? "<root>" : path;
    }

}
