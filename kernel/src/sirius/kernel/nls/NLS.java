/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.nls;

import com.google.common.collect.Sets;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import sirius.kernel.Classpath;
import sirius.kernel.Sirius;
import sirius.kernel.async.CallContext;
import sirius.kernel.commons.AdvancedDateParser;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Value;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.Time;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Native Language Support used by the framework.
 * <p>
 * This class provides a translation engine ({@link #get(String)}, {@link #safeGet(String, String, String)},
 * {@link #fmtr(String)}). It also provides access to the current language via {@link #getCurrentLang()} and to the
 * default language ({@link #getDefaultLanguage()}. Most of the methods come in two version, one which accepts a
 * <tt>lang</tt> parameter and another which uses the currently active language.
 * </p>
 * <p>
 * Additionally this class provides conversion methods to and from <tt>String</tt>. The most prominent ones are
 * {@link #toUserString(Object)} and {@link #toMachineString(Object)} along with their equivalent parse methods.
 * Although some conversions, especially <tt>toMachineString</tt> or <tt>formatSize</tt> are not language dependent,
 * those are kept in this class, to keep all conversion methods together.
 * </p>
 * <p>
 * <tt>Babelfish</tt> is used as translation engine and responsible for loading all provided .properties files.
 * </p>
 * <p>
 * <b>Configuration</b>
 * <ul>
 * <li><b>nls.defaultLanguage:</b> Sets the two-letter code used as default language</li>
 * <li><b>nls.language:</b> Sets an array of two-letter codes which enumerate all supported languages</li>
 * </ul>
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @see Babelfish
 * @since 2013/08
 */
public class NLS {

    private static final Babelfish blubb = new Babelfish();
    private static String defaultLanguage;
    private static Set<String> supportedLanguages;

    /**
     * Returns the currently active language as two-letter code.
     *
     * @return a two-letter code of the currently active language, as defined in
     *         {@link sirius.kernel.async.CallContext#getLang()}
     */
    public static String getCurrentLang() {
        return CallContext.getCurrent().getLang();
    }

    /**
     * Returns the two-letter code of the default language. Provided via the config in <code>nls.defaultLanguage</code>
     *
     * @return the language code of the default language
     */
    public static String getDefaultLanguage() {
        if (defaultLanguage == null && Sirius.getConfig() != null) {
            defaultLanguage = Sirius.getConfig().getString("nls.defaultLanguage");
        }
        // Returns the default language or (for very early access we default to en)
        return defaultLanguage == null ? "en" : defaultLanguage;
    }

    /**
     * Returns a list of two-letter codes enumerating all supported languages. Provided via the config in
     * <code>nls.languages</code>
     *
     * @return a list of supported language codes
     */
    public static Set<String> getSupportedLanguages() {
        if (supportedLanguages == null && Sirius.getConfig() != null) {
            supportedLanguages = Sets.newLinkedHashSet(Sirius.getConfig().getStringList("nls.languages"));
        }
        // Returns the default language or (for very early access we default to en)
        return supportedLanguages == null ? Collections.singleton("en") : Collections.unmodifiableSet(supportedLanguages);
    }

    /**
     * Determines if the given language code is supported or not.
     *
     * @param twoLetterLanguageCode the language as two-letter code
     * @return <tt>true</tt> if the language is listed in <tt>nls.langauges</tt>, <tt>false</tt> otherwise.
     */
    public static boolean isSupportedLanguage(String twoLetterLanguageCode) {
        return getSupportedLanguages().contains(twoLetterLanguageCode);
    }

    /**
     * Initializes the engine based on the given classpath
     *
     * @param classpath the classpath used to discover all .properties files
     */
    public static void init(Classpath classpath) {
        blubb.init(classpath);
    }

    /**
     * Provides direct access to the translation engine to supply new properties or inspect current ones.
     *
     * @return the internally used translation engine
     */
    public static Babelfish getTranslationEngine() {
        return blubb;
    }

    /**
     * Returns a translated text for the given <tt>property</tt> and the currently active language.
     * <p>
     * If no translation is found, the translation for the default language is used. If still no translation is
     * found, the property itself is returned.
     * </p>
     *
     * @param property the key for which a translation is requested
     * @return a translated string for the current language (or for the default language if no translation was found)
     *         or the property itself if no translation for neither of both languages is available.
     */
    public static String get(String property) {
        return blubb.get(property, null, true).translate(getCurrentLang());
    }

    /**
     * Returns a translated text for the given property in the given language.
     * <p>
     * The same fallback rules as for {@link #get(String)} apply.
     * </p>
     *
     * @param property the key for which a translation is requested
     * @param lang     a two-letter language code for which the translation is requested
     * @return a translated string in the requested language or a fallback value if no translation was found
     */
    public static String get(String property, String lang) {
        return blubb.get(property, null, true).translate(lang);
    }

    /**
     * Returns a translated text for the given <tt>property</tt> in the given language
     * or <tt>null</tt> if no translation was found.
     * <p>
     * The same fallback rules as for {@link #get(String, String)} apply. However, if no translation
     * </p>
     *
     * @param property the key for which a translation is requested
     * @param lang     a two-letter language code for which the translation is requested
     * @return a translated text in the requested language (or in the default language if no translation for the given
     *         language was found). Returns <tt>null</tt> if no translation for this property exists at all.
     */
    public static String getIfExists(String property, String lang) {
        Translation translation = blubb.get(property, null, false);
        if (translation == null) {
            return null;
        }
        return translation.translate(lang);
    }

    /**
     * Returns a translated text for the given <tt>property</tt> or for the given <tt>fallback</tt>, if no translation
     * for <tt>property</tt> was found.
     *
     * @param property the primary key for which a translation is requested
     * @param fallback the fallback key for which a translation is requested
     * @param lang     a two-letter language code for which the translation is requested
     * @return a translated text in the requested language for the given property, or for the given fallback. If either
     *         of both doesn't provide a translation for the given language, the translation for the default
     *         language is returned. If neither of both keys exist <tt>property</tt> will be returned.
     */
    public static String safeGet(String property, String fallback, String lang) {
        return blubb.get(property, fallback, true).translate(lang);
    }

    /**
     * Returns a translated text for the given <tt>property</tt> or for the given <tt>fallback</tt>, if no translation
     * for <tt>property</tt> was found.
     *
     * @param property the primary key for which a translation is requested
     * @param fallback the fallback key for which a translation is requested
     * @return a translated text in the current language for the given property, or for the given fallback. If either
     *         of both doesn't provide a translation for the given language, the translation for the default
     *         language is returned. If neither of both keys exist <tt>property</tt> will be returned.
     */
    public static String safeGet(String property, String fallback) {
        return blubb.get(property, fallback, true).translate(getCurrentLang());
    }

    /**
     * Creates a formatted using the pattern supplied by the translation value for the given <tt>property</tt>.
     *
     * @param property the property to used to retrieve a translated pattern
     * @return a <tt>Formatter</tt> initialized with the translated text of the given property
     */
    public static Formatter fmtr(String property) {
        return Formatter.create(get(property), getCurrentLang());
    }

    /**
     * Formats a translated string by replacing templates with the given parameters.
     * <p>
     * Uses the same format as {@link Strings#apply(String, Object...)}. However, this method should only be
     * used for very simple strings. {@link #fmtr(String)} provides support for named parameters, which are easier
     * to translate.
     * </p>
     *
     * @param key    the key used to lookup the translated text
     * @param params parameters used to replace arguments
     * @return a translated and formatted string as defined in <tt>Strings.apply</tt>
     */
    public static String apply(String key, Object... params) {
        return Strings.apply(get(key), params);
    }

    /**
     * Marks a string as deliberately not translated.
     * <p>
     * Can be used to signal that a string needs no internationalization as it is only used on rare cases etc.
     * </p>
     *
     * @param s the text which will be used as output
     * @return the given value for s
     */
    public static String nonNLS(String s) {
        return s;
    }

    /**
     * Provides access to commonly used keys.
     */
    public static enum CommonKeys {
        // Common terms
        YES, NO, OK, CANCEL, NAME, EDIT, DELETE, SEARCH, SEARCHKEY, REFRESH, CLOSE, DESCRIPTION, SAVE, NEW, BACK, FILTER,
        // Weekdays
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY,
        // Months
        JANUARY, FEBRUARY, MARCH, APRIL, MAY, JUNE, JULY, AUGUST, SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER;

        /**
         * Returns the fully qualified key to retrieve the translation
         *
         * @return the fully qualified key which can be supplied to <tt>NLS.get</tt>
         */
        public String key() {
            return "NLS." + name().toLowerCase();
        }

        /**
         * Returns the translation for this key in the current language.
         *
         * @return the translation for this key
         */
        public String translated() {
            return get(key());
        }
    }

    /**
     * Converts a given integer (<code>Calendar.Monday...Calendar.Sunday</code>) into textual their representation.
     *
     * @param day the weekday to be translated. Use constants {@link Calendar#MONDAY} etc.
     * @return the name of the given weekday in the current language
     *         or <code>""</code> if an invalid index was given
     */
    public static String getDayOfWeek(int day) {
        switch (day) {
            case Calendar.MONDAY:
                return CommonKeys.MONDAY.translated();
            case Calendar.TUESDAY:
                return CommonKeys.TUESDAY.translated();
            case Calendar.WEDNESDAY:
                return CommonKeys.WEDNESDAY.translated();
            case Calendar.THURSDAY:
                return CommonKeys.THURSDAY.translated();
            case Calendar.FRIDAY:
                return CommonKeys.FRIDAY.translated();
            case Calendar.SATURDAY:
                return CommonKeys.SATURDAY.translated();
            case Calendar.SUNDAY:
                return CommonKeys.SUNDAY.translated();
        }
        return "";
    }

    /**
     * Returns a two letter abbreviation of the name of the given day, like <code>"Mo"</code>.
     *
     * @param day the weekday to be translated. Use constants {@link Calendar#MONDAY} etc.
     * @return returns the first two letters of the name
     *         or <code>""</code> if the given index was invalid.
     */
    public static String getDayOfWeekShort(int day) {
        return Value.of(getDayOfWeek(day)).substring(0, 2);
    }

    /**
     * Returns the name of the given month in the current language
     *
     * @param month the month which name is requested. Use constants like {@link Calendar#JANUARY}, since the
     *              developers of this API chose quite brain-dead indices (January is 0....).
     * @return the name of the given month translated in the current language
     *         or <code>""</code> if an invalid index was given
     */
    public static String getMonthName(int month) {
        switch (month) {
            case Calendar.JANUARY:
                return CommonKeys.JANUARY.translated();
            case Calendar.FEBRUARY:
                return CommonKeys.FEBRUARY.translated();
            case Calendar.MARCH:
                return CommonKeys.MARCH.translated();
            case Calendar.APRIL:
                return CommonKeys.APRIL.translated();
            case Calendar.MAY:
                return CommonKeys.MAY.translated();
            case Calendar.JUNE:
                return CommonKeys.JUNE.translated();
            case Calendar.JULY:
                return CommonKeys.JULY.translated();
            case Calendar.AUGUST:
                return CommonKeys.AUGUST.translated();
            case Calendar.SEPTEMBER:
                return CommonKeys.SEPTEMBER.translated();
            case Calendar.OCTOBER:
                return CommonKeys.OCTOBER.translated();
            case Calendar.NOVEMBER:
                return CommonKeys.NOVEMBER.translated();
            case Calendar.DECEMBER:
                return CommonKeys.DECEMBER.translated();
        }
        return "";
    }

    /**
     * Returns a three letter abbreviation of the name of the given month, like <code>"Mo"</code>.
     *
     * @param month the month to be translated. Use constants {@link Calendar#JANUARY} etc.
     * @return returns the first three letters of the name
     *         or <code>""</code> if the given index was invalid.
     */
    public static String getMonthNameShort(int month) {
        String result = getMonthName(month);
        if (result.length() > 4) {
            result = result.substring(0, 3);
        }
        return result;
    }

    /**
     * Returns the date format for the given language.
     *
     * @param lang the language for which the format is requested
     * @return a format initialized with the pattern described by the given language
     */
    public static SimpleDateFormat getDateFormat(String lang) {
        return new SimpleDateFormat(get("NLS.patternDate", lang));
    }

    /**
     * Returns the full time format (with seconds) for the given language.
     *
     * @param lang the language for which the format is requested
     * @return a format initialized with the pattern described by the given language
     */
    public static SimpleDateFormat getFullTimeFormat(String lang) {
        return new SimpleDateFormat(get("NLS.patternFullTime", lang));
    }

    /**
     * Returns the time format (without seconds) for the given language.
     *
     * @param lang the language for which the format is requested
     * @return a format initialized with the pattern described by the given language
     */
    public static SimpleDateFormat getTimeFormat(String lang) {
        return new SimpleDateFormat(get("NLS.patternTime", lang));
    }

    /**
     * Returns the date and time format (without seconds) for the given language.
     *
     * @param lang the language for which the format is requested
     * @return a format initialized with the pattern described by the given language
     */
    public static SimpleDateFormat getDateTimeFormat(String lang) {
        return new SimpleDateFormat(get("NLS.patternDateTime", lang));
    }

    /**
     * Returns the format for the given language to format decimal numbers
     *
     * @param lang the language for which the format is requested
     * @return a format initialized with the pattern described by the given language
     */
    public static java.text.NumberFormat getDecimalFormat(String lang) {
        return new DecimalFormat(get("NLS.patternDecimal", lang), getDecimalFormatSymbols(lang));
    }

    /**
     * Returns the decimal format symbols for the current language
     *
     * @return the decimal format symbols like thousands separator or decimal separator
     *         as described by the current language
     */
    public static DecimalFormatSymbols getDecimalFormatSymbols() {
        return getDecimalFormatSymbols(getCurrentLang());
    }

    /**
     * Returns the decimal format symbols for the given language
     *
     * @param lang the two-letter code of the language for which the decimal format symbols should be returned
     * @return the decimal format symbols like thousands separator or decimal separator
     *         as described by the given language
     */
    public static DecimalFormatSymbols getDecimalFormatSymbols(String lang) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols();
        sym.setGroupingSeparator(get("NLS.groupingSeparator", lang).charAt(0));
        sym.setDecimalSeparator(get("NLS.decimalSeparator", lang).charAt(0));
        return sym;
    }

    /**
     * Formats the given data in a language independent format.
     *
     * @param data the input data which should be converted to string
     * @return string representation of the given object, which can be parsed by
     *         {@link #parseMachineString(Class, String)} independently of the language settings
     */
    public static String toMachineString(Object data) {
        if (data == null) {
            return "";
        }
        if (data instanceof String) {
            return ((String) data).trim();
        }
        if (data instanceof Boolean) {
            return data.toString();
        }
        if (data instanceof Calendar) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return format.format(((Calendar) data).getTime());
        }
        if (data instanceof Time) {
            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
            return format.format(new Date(((Time) data).getTime()));
        }
        if (data instanceof Date) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return format.format((Date) data);
        }
        if (data instanceof LocalDate) {
            return ((LocalDate) data).toString("yyyy-MM-dd", Locale.ENGLISH);
        }
        if (data instanceof DateTime) {
            return ((DateTime) data).toString("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        }
        if (data instanceof Integer) {
            return String.valueOf(data);
        }
        if (data instanceof Long) {
            return String.valueOf(data);
        }
        if (data instanceof BigDecimal) {
            return ((BigDecimal) data).toPlainString();
        }
        if (data instanceof Double) {
            return String.valueOf(data);
        }
        if (data.getClass().isEnum()) {
            return ((Enum<?>) data).name();
        }
        if (data instanceof Float) {
            return String.valueOf(data);
        }
        if (data instanceof Throwable) {
            StringWriter writer = new StringWriter();
            PrintWriter pw = new PrintWriter(writer);
            ((Throwable) data).printStackTrace(pw);
            String result = writer.toString();
            pw.close();
            return result;
        }
        return String.valueOf(data);
    }


    /**
     * Returns a string representation of the given number in an english format, that is,
     * using a dot as decimal separator.
     *
     * @param number the number to be converted
     * @return a string representation of the given number using a dot as decimal separator
     *         independent of the current language settings
     */
    public static String toEnglishRepresentation(Number number) {
        return new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(number);
    }

    /**
     * Formats the given data according to the format rules of the current language
     *
     * @param object the object to be converted to a string
     * @return a string representation of the given object, formatted by the language settings of the current language
     */
    public static String toUserString(Object object) {
        return toUserString(object, getCurrentLang(), false);
    }


    /**
     * Formats the given data according to the format rules of the current language
     *
     * @param object             the object to be converted to a string
     * @param fullDateConversion controls whether dates will also include their time fields in the result string
     * @return a string representation of the given object, formatted by the language settings of the current language
     */
    public static String toUserString(Object object, boolean fullDateConversion) {
        return toUserString(object, getCurrentLang(), fullDateConversion);
    }

    /**
     * Formats the given data according to the format rules of the given language
     *
     * @param data               the object to be converted to a string
     * @param lang               a two-letter language code for which the translation is requested
     * @param fullDateConversion controls whether dates will also include their time fields in the result string
     * @return a string representation of the given object, formatted by the language settings of the current language
     */
    public static String toUserString(Object data, String lang, boolean fullDateConversion) {
        if (data == null) {
            return "";
        }
        if (data instanceof String) {
            return ((String) data).trim();
        }
        if (data instanceof Boolean) {
            if (((Boolean) data).booleanValue()) {
                return CommonKeys.YES.translated();
            } else {
                return CommonKeys.NO.translated();
            }
        }
        if (data instanceof Calendar) {
            if (fullDateConversion) {
                return getDateTimeFormat(lang).format(((Calendar) data).getTime());
            } else {
                return getDateFormat(lang).format(((Calendar) data).getTime());
            }
        }
        if (data instanceof Time) {
            if (fullDateConversion) {
                return getFullTimeFormat(lang).format(new Date(((Time) data).getTime()));
            } else {
                return getTimeFormat(lang).format(new Date(((Time) data).getTime()));
            }
        }
        if (data instanceof Date) {
            if (fullDateConversion) {
                return getDateTimeFormat(lang).format((Date) data);
            } else {
                return getDateFormat(lang).format((Date) data);
            }
        }
        if (data instanceof DateTime) {
            if (fullDateConversion) {
                return getDateTimeFormat(lang).format(((DateTime) data).toDate());
            } else {
                return getDateFormat(lang).format(((DateTime) data).toDate());
            }
        }
        if (data instanceof LocalDate) {
            if (fullDateConversion) {
                return getDateTimeFormat(lang).format(((LocalDate) data).toDate());
            } else {
                return getDateFormat(lang).format(((LocalDate) data).toDate());
            }
        }
        if (data instanceof LocalTime) {
            return getTimeFormat(lang).format(((LocalTime) data).toDateTimeToday().toDate());
        }
        if (data instanceof Integer) {
            return String.valueOf(data);
        }
        if (data instanceof Long) {
            return String.valueOf(data);
        }
        if (data instanceof BigDecimal) {
            return getDecimalFormat(lang).format(((BigDecimal) data).doubleValue());
        }
        if (data instanceof Double) {
            return getDecimalFormat(lang).format(data);
        }
        if (data instanceof Float) {
            return getDecimalFormat(lang).format(data);
        }
        if (data instanceof Throwable) {
            StringWriter writer = new StringWriter();
            PrintWriter pw = new PrintWriter(writer);
            ((Throwable) data).printStackTrace(pw);
            String result = writer.toString();
            pw.close();
            return result;
        }
        return String.valueOf(data);

    }

    /**
     * Converts dates to a "human" format (today, yesterday, tomorrow).
     * <p>
     * Everything but today, yesterday and tomorrow will be converted to a string representation formatted by
     * using the date format of the current language.
     * </p>
     *
     * @param object the date to be formatted. If the given object isn't a known date class
     *               (Date, Calendar or Joda-Class), <tt>toUserString</tt> is called
     * @return a date string which a human would use in common sentences
     */
    public static String toSpokenDate(Object object) {
        if (object == null) {
            return toUserString(object, getCurrentLang(), false);
        }
        if (object instanceof Date) {
            object = LocalDate.fromDateFields((Date) object);
        }
        if (object instanceof DateTime) {
            object = ((DateTime) object).toLocalDate();
        }
        if (object instanceof DateMidnight) {
            object = ((DateMidnight) object).toLocalDate();
        }
        if (object instanceof Calendar) {
            object = LocalDate.fromCalendarFields((Calendar) object);
        }
        if (object instanceof LocalDate) {
            LocalDate reference = LocalDate.now();
            if (reference.equals(object)) {
                return NLS.get("NLS.today");
            }
            reference = reference.plusDays(1);
            if (reference.equals(object)) {
                return NLS.get("NLS.tomorrow");
            }
            reference = reference.minusDays(1);
            if (reference.equals(object)) {
                return NLS.get("NLS.yesterday");
            }
        }
        return toUserString(object, getCurrentLang(), false);
    }

    /**
     * Parses the given string by expecting a machine independent format.
     * <p>
     * This can parse all strings generated by <tt>toMachineString</tt>
     * </p>
     *
     * @param clazz the expected class of the value to be parsed
     * @param value the string to be parsed
     * @return an instance of <tt>clazz</tt> representing the parsed string or <tt>null</tt> if value was empty.
     * @throws IllegalArgumentException if the given input was not well formed or if instances of <tt>clazz</tt>
     *                                  cannot be created. The thrown exception has a translated error message which
     *                                  can be directly presented to the user.
     */
    @SuppressWarnings("unchecked")
    public static <V> V parseMachineString(Class<V> clazz, String value) {
        if (Strings.isEmpty(value)) {
            return null;
        }
        if (String.class.equals(clazz)) {
            return (V) value;
        }
        if (Integer.class.equals(clazz) || int.class.equals(clazz)) {
            try {
                return (V) Integer.valueOf(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidNumber").set("value", value).format(), e);
            }
        }
        if (Long.class.equals(clazz) || long.class.equals(clazz)) {
            try {
                return (V) Long.valueOf(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidNumber").set("value", value).format(), e);
            }
        }
        if (Float.class.equals(clazz) || float.class.equals(clazz)) {
            try {
                Double result = Double.valueOf(value);
                return (result == null) ? null : (V) Float.valueOf(result.floatValue());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidDecimalNumber").set("value", value).format(), e);

            }
        }
        if (Double.class.equals(clazz) || double.class.equals(clazz)) {
            try {
                return (V) Double.valueOf(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidDecimalNumber").set("value", value).format(), e);
            }
        }
        if (BigDecimal.class.equals(clazz)) {
            try {
                return (V) new BigDecimal(Double.valueOf(value));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidDecimalNumber").set("value", value).format(), e);
            }
        }
        if (Boolean.class.equals(clazz) || boolean.class.equals(clazz)) {
            return (V) new Boolean(Boolean.parseBoolean(value));
        }
        if (String.class.equals(clazz)) {
            return (V) value;
        }
        if (Date.class.equals(clazz)) {
            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                return (V) format.parse(value);
            } catch (ParseException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidDate").set("value", value)
                                                           .set("format", "yyyy-MM-dd HH:mm:ss")
                                                           .format(), e);
            }
        }
        if (Time.class.equals(clazz)) {
            try {
                SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
                return (V) new Time(format.parse(value).getTime());
            } catch (ParseException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidDate").set("value", value)
                                                           .set("format", "HH:mm:ss")
                                                           .format(), e);
            }
        }
        if (DateTime.class.equals(clazz)) {
            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                return (V) new DateTime(format.parse(value));
            } catch (ParseException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidDate").set("value", value)
                                                           .set("format", "yyyy-MM-dd HH:mm:ss")
                                                           .format(), e);
            }
        }
        if (LocalDate.class.equals(clazz)) {
            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                return (V) new LocalDate(format.parse(value));
            } catch (ParseException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidDate").set("value", value)
                                                           .set("format", "yyyy-MM-dd HH:mm:ss")
                                                           .format(), e);
            }
        }
        if (LocalTime.class.equals(clazz)) {
            try {
                SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
                return (V) new LocalTime(format.parse(value));
            } catch (ParseException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidDate").set("value", value)
                                                           .set("format", "HH:mm:ss")
                                                           .format(), e);
            }
        }
        if (Calendar.class.equals(clazz)) {
            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                return (V) format.parse(value);
            } catch (ParseException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidDate").set("value", value)
                                                           .set("format", "yyyy-MM-dd HH:mm:ss")
                                                           .format(), e);
            }
        }
        throw new IllegalArgumentException(fmtr("NLS.parseError").set("type", clazz).format());
    }

    /**
     * Parses the given string by expecting a format as defined by the given language.
     *
     * @param clazz the expected class of the value to be parsed
     * @param value the string to be parsed
     * @param lang  the two-letter code of the language which format should be used
     * @return an instance of <tt>clazz</tt> representing the parsed string or <tt>null</tt> if value was empty.
     * @throws IllegalArgumentException if the given input was not well formed or if instances of <tt>clazz</tt>
     *                                  cannot be created. The thrown exception has a translated error message which
     *                                  can be directly presented to the user.
     */
    @SuppressWarnings("unchecked")
    public static <V> V parseUserString(Class<V> clazz, String value, String lang) {
        if (Strings.isEmpty(value)) {
            return null;
        }
        if (String.class.equals(clazz)) {
            return (V) value;
        }
        if (Integer.class.equals(clazz) || int.class.equals(clazz)) {
            try {
                return (V) Integer.valueOf(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidNumber").set("value", value).format(), e);
            }
        }
        if (Long.class.equals(clazz) || long.class.equals(clazz)) {
            try {
                return (V) Long.valueOf(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidNumber").set("value", value).format(), e);
            }
        }
        if (Float.class.equals(clazz) || float.class.equals(clazz)) {
            try {
                try {
                    // If there is exactly one "." in the pattern and no "," and
                    // we have less then 3 digits behind the "." we treat this
                    // as english decimal format and not as german grouping
                    // separator.
                    if (".".equals(NLS.get("NLS.groupingSeparator")) && value.contains(".") && !value.contains(",") && value
                            .indexOf(".") == value.lastIndexOf(".") && value.indexOf(".") > value.length() - 4) {
                        try {
                            Double result = Double.valueOf(value);
                            return (result == null) ? null : (V) Float.valueOf(result.floatValue());
                        } catch (Exception e) {
                            /* IGNORE, TRY REAL FORMAT */
                        }
                    }
                    return (V) (Float) getDecimalFormat(lang).parse(value).floatValue();
                } catch (ParseException e) {
                    Double result = Double.valueOf(value);
                    return (result == null) ? null : (V) Float.valueOf(result.floatValue());
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidDecimalNumber").set("value", value).format(), e);
            }
        }
        if (Double.class.equals(clazz) || double.class.equals(clazz)) {
            try {
                try {
                    // If there is exactly one "." in the pattern and no "," and
                    // we have less then 3 digits behind the "." we treat this
                    // as english decimal format and not as german grouping
                    // separator.
                    if (".".equals(NLS.get("NLS.groupingSeparator")) && value.contains(".") && !value.contains(",") && value
                            .indexOf(".") == value.lastIndexOf(".") && value.indexOf(".") > value.length() - 4) {
                        try {
                            return (V) (Double) Double.valueOf(value);
                        } catch (Exception e) {
                            /* IGNORE, TRY REAL FORMAT */
                        }
                    }
                    return (V) (Double) getDecimalFormat(lang).parse(value).doubleValue();
                } catch (ParseException e) {
                    return (V) Double.valueOf(value);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(fmtr("NLS.errInvalidDecimalNumber").set("value", value).format(), e);
            }
        }
        if (Boolean.class.equals(clazz) || boolean.class.equals(clazz)) {
            if (CommonKeys.YES.translated().equalsIgnoreCase(value)) {
                return (V) Boolean.TRUE;
            } else if (CommonKeys.NO.translated().equalsIgnoreCase(value)) {
                return (V) Boolean.FALSE;
            }
            return (V) new Boolean(Boolean.parseBoolean(value));
        }
        if (Date.class.equals(clazz)) {
            try {
                AdvancedDateParser parser = new AdvancedDateParser(lang);
                return (V) parser.parse(value).getCalendar().getTime();
            } catch (ParseException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }
        if (Time.class.equals(clazz)) {
            try {
                return (V) new Time(getFullTimeFormat(lang).parse(value).getTime());
            } catch (ParseException e) {
                try {
                    return (V) new Time(getTimeFormat(lang).parse(value).getTime());
                } catch (ParseException ex) {
                    throw new IllegalArgumentException(fmtr("NLS.errInvalidTime").set("value", value).format(), ex);
                }
            }
        }
        if (DateTime.class.equals(clazz)) {
            try {
                AdvancedDateParser parser = new AdvancedDateParser(lang);
                return (V) new DateTime(parser.parse(value).getCalendar());
            } catch (ParseException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }
        if (LocalDate.class.equals(clazz)) {
            try {
                AdvancedDateParser parser = new AdvancedDateParser(lang);
                return (V) new LocalDate(parser.parse(value).getCalendar());
            } catch (ParseException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }
        if (LocalTime.class.equals(clazz)) {
            try {
                return (V) new LocalTime(getFullTimeFormat(lang).parse(value));
            } catch (ParseException e) {
                try {
                    return (V) new LocalTime(getTimeFormat(lang).parse(value));
                } catch (ParseException ex) {
                    throw new IllegalArgumentException(fmtr("NLS.errInvalidTime").set("value", value).format(), ex);
                }
            }
        }
        if (Calendar.class.equals(clazz)) {
            try {
                AdvancedDateParser parser = new AdvancedDateParser(lang);
                return (V) parser.parse(value).getCalendar();
            } catch (ParseException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }
        if (AdvancedDateParser.DateSelection.class.equals(clazz)) {
            try {
                AdvancedDateParser parser = new AdvancedDateParser(lang);
                return (V) parser.parse(value);
            } catch (ParseException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }
        throw new IllegalArgumentException(fmtr("NLS.parseError").set("type", clazz).format());
    }

    /**
     * Parses the given string by expecting a format as defined by the current language.
     *
     * @param clazz  the expected class of the value to be parsed
     * @param string the string to be parsed
     * @return an instance of <tt>clazz</tt> representing the parsed string or <tt>null</tt> if value was empty.
     * @throws IllegalArgumentException if the given input was not well formed or if instances of <tt>clazz</tt>
     *                                  cannot be created. The thrown exception has a translated error message which
     *                                  can be directly presented to the user.
     */
    public static <V> V parseUserString(Class<V> clazz, String string) {
        return parseUserString(clazz, string, getCurrentLang());
    }

    private final static long SECOND = 1000;
    private final static long MINUTE = 60 * SECOND;
    private final static long HOUR = 60 * MINUTE;
    private final static long DAY = 24 * HOUR;

    /**
     * Converts a given time range in milliseconds to a human readable format using the current language
     *
     * @param duration       the duration in milliseconds
     * @param includeSeconds determines whether to include seconds or to ignore everything below minutes
     * @param includeMillis  determines whether to include milli seconds or to ignore everything below seconds
     * @return a string representation of the given duration in days, hours, minutes and,
     *         if enabled, seconds and milliseconds
     */
    public static String convertDuration(long duration, boolean includeSeconds, boolean includeMillis) {
        StringBuilder result = new StringBuilder();
        if (duration > DAY) {
            long value = duration / DAY;
            if (value == 1) {
                result.append(NLS.apply("NLS.day", value));
            } else {
                result.append(NLS.apply("NLS.days", value));
            }
            duration = duration % DAY;
        }
        if (duration > HOUR) {
            if (result.length() > 0) {
                result.append(", ");
            }
            long value = duration / HOUR;
            if (value == 1) {
                result.append(NLS.apply("NLS.hour", value));
            } else {
                result.append(NLS.apply("NLS.hours", value));
            }
            duration = duration % HOUR;
        }
        if (duration > MINUTE || (!includeSeconds && duration > 0)) {
            if (result.length() > 0) {
                result.append(", ");
            }
            long value = duration / MINUTE;
            if (duration % MINUTE > 0) {
                value++;
            }
            if (value == 1) {
                result.append(NLS.apply("NLS.minute", value));
            } else {
                result.append(NLS.apply("NLS.minutes", value));
            }
            duration = duration % MINUTE;
        }
        if (includeSeconds) {
            if (duration > SECOND || (!includeMillis && duration > 0)) {
                if (result.length() > 0) {
                    result.append(", ");
                }
                long value = duration / SECOND;
                if (value == 1) {
                    result.append(NLS.apply("NLS.second", value));
                } else {
                    result.append(NLS.apply("NLS.seconds", value));
                }
                duration = duration % SECOND;
            }
            if (includeMillis) {
                if (duration > 0) {
                    if (result.length() > 0) {
                        result.append(", ");
                    }
                    if (duration == 1) {
                        result.append(NLS.apply("NLS.millisecond", duration));
                    } else {
                        result.append(NLS.apply("NLS.milliseconds", duration));
                    }
                }
            }
        }
        return result.toString();
    }

    /**
     * Converts the given duration in milliseconds including seconds and milliseconds
     * <p>
     * This is a boilerplate method for {@link #convertDuration(long, boolean, boolean)} with
     * <tt>includeSeconds</tt> and <tt>includeMillis</tt> set to <tt>true</tt>.
     * </p>
     *
     * @param duration the duration in milliseconds
     * @return a string representation of the given duration in days, hours, minutes, seconds and milliseconds
     */
    public static String convertDuration(long duration) {
        return convertDuration(duration, true, true);
    }

    /**
     * Outputs integer numbers without decimals, but fractional numbers with two digits.
     * <p>
     * Discards fractional parts which absolute value is less or equal to <code>0.00001</code>.
     * </p>
     *
     * @param number the number to be rounded
     * @return a string representation using the current languages decimal format.
     *         Rounds fractional parts less or equal to <code>0.00001</code>
     */
    public static String smartRound(double number) {
        if (Math.abs(Math.floor(number) - number) > 0.000001D) {
            return NLS.toUserString(number);
        } else {
            return String.valueOf(Math.round(number));
        }
    }

    /**
     * Converts a file or byte size.
     * <p>
     * Supports sizes up to petabyte. Uses conventional abbreviations like KB, MB but technically returns KiB or
     * MiB (this is values divided by 1024 instead of 1000).
     * </p>
     *
     * @param size the size to format in bytes
     * @return an english representation (using dot as decimal separator) along with one of the known abbreviations:
     *         <code>Byes, KB, MB, GB, TB, PB</code>.
     */
    public static String formatSize(long size) {
        int index = 0;
        while (size > 1024 && index < UNITS.length - 1) {
            size = size / 1024;
            index++;
        }
        return NLS.toEnglishRepresentation(size) + " " + UNITS[index];
    }

    private static final String[] UNITS = new String[]{"Bytes", "KB", "MB", "GB", "TB", "PB"};

}
