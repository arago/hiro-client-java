package co.arago.hiro.client.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class RequiredFieldChecker {
    /**
     * Check for null values. Used with required parameters of builder fields.
     *
     * @param item The item to check for null
     * @param name Name of the item for the exception message
     * @return The item itself
     * @throws NullPointerException when item == null
     */
    public static <N> N notNull(N item, String name) {
        return Objects.requireNonNull(item, "Field '${name}' is required and cannot be null.");
    }

    /**
     * Check for empty string values. Used with required parameters of builder fields.
     *
     * @param item The item to check for
     * @param name Name of the item for the exception message
     * @return The item itself
     * @throws IllegalArgumentException when item == null
     */
    public static String notEmpty(String item, String name) {
        if (StringUtils.isEmpty(item))
            throw new IllegalArgumentException("Field '" + name + "' cannot be empty.");
        return item;
    }

    /**
     * Check for empty collection values. Used with required parameters of builder fields.
     *
     * @param item The item to check for
     * @param name Name of the item for the exception message
     * @return The item itself
     * @throws IllegalArgumentException when item == null
     */
    public static Collection<?> notEmpty(Collection<?> item, String name) {
        if (item == null || item.isEmpty())
            throw new IllegalArgumentException("Collection '" + name + "' cannot be empty.");
        return item;
    }

    /**
     * Check for empty map values. Used with required parameters of builder fields.
     *
     * @param item The item to check for
     * @param name Name of the item for the exception message
     * @return The item itself
     * @throws IllegalArgumentException when item == null
     */
    public static Map<?, ?> notEmpty(Map<?, ?> item, String name) {
        if (item == null || item.isEmpty())
            throw new IllegalArgumentException("Map '" + name + "' cannot be empty.");
        return item;
    }

    /**
     * Check for blank string values. Used with required parameters of builder fields.
     *
     * @param item The item to check for
     * @param name Name of the item for the exception message
     * @return The item itself
     * @throws IllegalArgumentException when item == null
     */
    public static String notBlank(String item, String name) {
        if (StringUtils.isBlank(item))
            throw new IllegalArgumentException("Field '" + name + "' cannot be blank.");
        return item;
    }

    /**
     * Create a exception for any custom error
     *
     * @param message The message to display.
     */
    public static void anyError(String message) {
        throw new IllegalArgumentException(message);
    }
}
