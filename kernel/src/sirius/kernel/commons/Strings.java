/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import com.google.common.base.Objects;
import org.jboss.netty.util.CharsetUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Provides various helper methods for dealing with Java <tt>Strings</tt>
 * <p>
 * The {@link Value} class provides some additional methods for working with nullable strings like
 * {@link Value#left(int)}, {@link Value#toLowerCase()} etc.
 * </p>
 * <p>
 * This class can and should not be instantiated, as all methods are static.
 * </p>
 *
 * @author aha
 * @see Value
 * @since 1.0
 */
public class Strings {

    /*
     * All methods are static, therefore no instances need to be created.
     */
    private Strings() {

    }

    /**
     * Checks if the string representation of the given object is "" or <tt>null</tt>.
     *
     * @param string the object which is to be checked
     * @return <tt>true</tt> if string is <tt>null</tt> or "", <tt>false</tt> otherwise
     */
    public static boolean isEmpty(Object string) {
        if (string == null) {
            return true;
        }
        return string.toString() == null || "".equals(string.toString());
    }

    /**
     * Checks if the string representation of the given object is neither "" nor <tt>null</tt>.
     *
     * @param string the object which is to be checked
     * @return <tt>true</tt> if string is not <tt>null</tt> and not "", <tt>false</tt> otherwise
     */
    public static boolean isFilled(Object string) {
        if (string == null) {
            return false;
        }
        return !"".equals(string.toString());
    }

    /**
     * Compares the given <tt>Strings</tt> while treating upper- and lowercase characters as equal.
     * <p>
     * This is essentially the same as <code>left.equalsIgnoreCase(right)</code>
     * while gracefully handling <tt>null</tt> values.
     * </p>
     *
     * @param left  the first string to be compared
     * @param right the second string to be compared with
     * @return <tt>true</tt> if both values are empty or if both strings are equal
     *         while ignoring their case - <tt>false</tt> otherwise
     */
    public static boolean equalIgnoreCase(String left, String right) {
        if (isEmpty(left)) {
            return isEmpty(right);
        }
        return left.equalsIgnoreCase(right);
    }

    /**
     * Compares the given <tt>Strings</tt> just like {@link String#compareTo(String)}
     * but with graceful handling for <tt>null</tt> values.
     *
     * @param left  the first string to be compared
     * @param right the second string to be compared with
     * @return <tt>true</tt> if both values are empty or if both strings are equal - <tt>false</tt> otherwise
     */
    public static boolean areEqual(Object left, Object right) {
        if (isEmpty(left) && isEmpty(right)) {
            return true;
        }
        return Objects.equal(left, right);
    }

    /**
     * Returns a string representation of the given object while gracefully handling <tt>null</tt> values.
     * <p>
     * Internally this method calls {@link Object#toString()}. For locale aware or locale fixed methods,
     * {@link sirius.kernel.nls.NLS#toUserString(Object)} and
     * {@link sirius.kernel.nls.NLS#toMachineString(Object)} can be used.
     * </p>
     *
     * @param object the object to be converted to string.
     * @return the string representation of the given object or <tt>null</tt> if <tt>object</tt> was null.
     */
    public static String toString(Object object) {
        return object == null ? null : object.toString();
    }

    /**
     * Formats the given pattern string <tt>format</tt> with the given <tt>arguments</tt>.
     * <p>
     * This is just a delegate to {@link String#format(String, Object...)}. It is however defined in this class to
     * forces all framework parts to use the same formatting mechanism (and not <tt>MessageFormat</tt> etc.).
     * </p>
     * <p>
     * This method is intended to be used for format short strings or non-translated log messages etc. For more
     * complex messages and especially for translated strings, a {@link sirius.kernel.nls.Formatter} should be
     * used.
     * </p>
     *
     * @param format    the format pattern to be used
     * @param arguments the parameters for be used for replacement
     * @return a formatted string as defined in <tt>String#format</tt>
     * @see String#format(String, Object...)
     * @see sirius.kernel.nls.Formatter
     * @see sirius.kernel.nls.NLS#fmtr(String)
     */
    public static String apply(String format, Object... arguments) {
        return String.format(format, arguments);
    }

    /**
     * Returns the first non empty value of the given array.
     * <p>
     * This can be used to provide a default value or to check several sources for a value, e.g.:
     * <code><pre>
     *         String s = Strings.firstFilled(System.getProperty("foo.test"),
     *                                        System.getProperty("test"),
     *                                        "default");
     *     </pre></code>
     * </p>
     *
     * @param values an array of string values to be scanned
     * @return the first value of values which is filled.
     *         Returns <tt>null</tt> if all are empty or if no values where passed in
     */
    public static String firstFilled(String... values) {
        if (values != null) {
            for (String s : values) {
                if (isFilled(s)) {
                    return s;
                }
            }
        }
        return null;
    }

    /**
     * Returns an url encoded representation of the given <tt>value</tt> with <tt>UTF-8</tt> as character encoding.
     *
     * @param value the value to be encoded.
     * @return an url encoded representation of value, using UTF-8 as character encoding.
     */
    public static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, CharsetUtil.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            // Cannot happen if Java-Version is > 1.4....

            return value;
        }
    }

    /**
     * Splits the given string at the first occurrence of the separator.
     * <p>
     * If the given input is empty, a tuple with <tt>null</tt> as first and second component will be returned.
     * </p>
     *
     * @param input     the input to be split
     * @param separator the separator used to split at
     * @return a <tt>Tuple</tt> containing the part before the separator as first
     *         and the part after the separator as second component
     */
    public static Tuple<String, String> split(String input, String separator) {
        Tuple<String, String> result = new Tuple<String, String>();
        if (isFilled(input)) {
            int idx = input.indexOf(separator);
            if (idx > -1) {
                result.setFirst(input.substring(0, idx));
                result.setSecond(input.substring(idx + separator.length()));
            } else {
                result.setFirst(input);
            }
        }
        return result;
    }
}
