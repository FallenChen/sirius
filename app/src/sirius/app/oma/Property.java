/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.app.oma;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import sirius.app.oma.annotations.*;
import sirius.app.oma.schema.Column;
import sirius.app.oma.schema.SQLKeywords;
import sirius.kernel.Sirius;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Value;
import sirius.kernel.health.Exceptions;
import sirius.kernel.nls.NLS;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Types;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 12.01.14
 * Time: 13:30
 * To change this template use File | Settings | File Templates.
 */
public class Property {
    public static final Property EMPTY = new Property();
    private String name;
    private boolean notNull;
    private Integer length;
    private Integer decimalPlaces;
    private Field field;
    private List<String> uniqueWithin;
    private Object defaultValue;
    private boolean ordinal;

    private Property() {

    }

    public Property(Field field, Entity defaultEntity) {
        this.field = field;
        this.ordinal = field.isAnnotationPresent(Ordinal.class);
        this.name = field.getName();
        if (field.isAnnotationPresent(Length.class)) {
            length = field.getAnnotation(Length.class).value();
            decimalPlaces = field.getAnnotation(Length.class).decimalPlaces();
        }
        if (field.getType().isPrimitive()) {
            notNull = true;
        } else if (field.isAnnotationPresent(NotNull.class)) {
            notNull = true;
        } else {
            notNull = false;
        }
        if (field.isAnnotationPresent(Unique.class)) {
            uniqueWithin = Collections.unmodifiableList(Arrays.asList(field.getAnnotation(Unique.class).within()));
        }
        field.setAccessible(true);
        try {
            this.defaultValue = field.get(defaultEntity);
        } catch (Throwable e) {
            Exceptions.handle()
                      .error(e)
                      .to(OMA.LOG)
                      .withSystemErrorMessage("Cannot determine default value of: %s",
                                              defaultEntity.getClass().getName())
                      .handle();
        }
        checkConsistency();
    }

    private void checkConsistency() {
        if (Sirius.isDev() && SQLKeywords.isReserved(getName())) {
            OMA.LOG.WARN("%s of %s is a reserved SQL keyword!", getName(), field.getDeclaringClass().getName());
        }
        if (field.getType().isPrimitive()) {
            return;
        }
        if (field.getType() == String.class) {
            if (length == null) {
                OMA.LOG
                   .WARN("The string property %s of %s needs an @Length specification! Defaulting to 255",
                         field.getDeclaringClass().getName(),
                         field.getName());
                length = 255;
            }
            return;
        }
        if (field.getType() == BigDecimal.class) {
            if (length == null) {
                OMA.LOG
                   .WARN("The numeric property %s of %s needs an @Length specification! Defaulting to 10,3",
                         field.getDeclaringClass().getName(),
                         field.getName());
                length = 10;
                decimalPlaces = 3;
            }
            return;
        }
        if (field.getType() == Integer.class || field.getType() == Long.class || field.getType() == Double.class || field
                .getType() == Float.class || field.getType() == Boolean.class) {
            return;
        }
        if (field.getType() == Date.class || field.getType() == Calendar.class || field.getType() == DateTime.class || field
                .getType() == LocalDate.class || field.getType() == LocalTime.class) {
            return;
        }
        if (field.getType().isEnum()) {
            if (length == null && !ordinal) {
                Exceptions.handle()
                          .to(OMA.LOG)
                          .withSystemErrorMessage(
                                  "The enum property %s of %s needs an @Length specification - or use @Ordinal to store the index value! Defaulting to 255",
                                  field.getDeclaringClass().getName(),
                                  field.getName())
                          .handle();
                length = 255;
            }
            return;
        }
        if (field.getType() == One.class) {
            if (!field.isAnnotationPresent(RefType.class)) {
                throw Exceptions.handle()
                                .to(OMA.LOG)
                                .withSystemErrorMessage("The property %s of %s is missing its @RefType annotation",
                                                        field.getName(),
                                                        field.getDeclaringClass().getName())
                                .handle();
            }
            return;
        }
        throw Exceptions.handle()
                        .to(OMA.LOG)
                        .withSystemErrorMessage("The property %s of %s has an unsupported field type: %s",
                                                field.getDeclaringClass().getName(),
                                                field.getName(),
                                                field.getType().getName())
                        .handle();

    }

    public String getName() {
        return name;
    }

    public <E extends Entity> Object writeDatabase(E entity) {
        try {
            return convertToDB(field.get(entity));
        } catch (Throwable e) {
            throw Exceptions.handle()
                            .to(OMA.LOG)
                            .error(e)
                            .withSystemErrorMessage("Cannot access property %s of %s: %s (%s)",
                                                    field.getName(),
                                                    field.getDeclaringClass().getName())
                            .handle();
        }
    }

    public Object convertToDB(Object o) {
        if (o == null) {
            return null;
        }
        if (field.getType() == Boolean.class || field.getType() == boolean.class) {
            if (((Boolean) o).booleanValue()) {
                return 1;
            }
            return 0;
        }
        if (field.getType().isEnum()) {
            if (ordinal) {
                return ((Enum<?>) o).ordinal();
            } else {
                return ((Enum<?>) o).name();
            }
        }
        return o;
    }

    public <E extends Entity> void readDatabase(E entity, Value value) {
        try {
            field.set(entity, convertToJava(value));
            entity.setFetchValue(getName(), value);
        } catch (Throwable e) {
            throw Exceptions.handle()
                            .to(OMA.LOG)
                            .error(e)
                            .withSystemErrorMessage("Cannot access property %s of %s: %s (%s)",
                                                    field.getName(),
                                                    field.getDeclaringClass().getName())
                            .handle();
        }
    }

    private Object convertToJava(Value val) {
        if (val.isNull()) {
            return null;
        }
        if (field.getType() == Boolean.class || field.getType() == boolean.class) {
            return (val.asInt(0) == 1);
        }

        if (field.getType().isEnum()) {
            if (ordinal) {
                int idx = val.asInt(-1);
                try {
                    return field.getType().getEnumConstants()[idx];
                } catch (ArrayIndexOutOfBoundsException e) {
                    return defaultValue;
                }
            } else {
                try {
                    return Enum.valueOf((Class<? extends Enum>) field.getType(), val.asString());
                } catch (IllegalArgumentException e) {
                    return defaultValue;
                }
            }
        }
        return val.get();

    }

    public boolean isNullable() {
        return !notNull;
    }

    public boolean isNotNull() {
        return notNull;
    }

    public boolean isUnique() {
        return uniqueWithin != null;
    }

    public List<String> getUniqueWithinFields() {
        return uniqueWithin;
    }

    public Integer getLength() {
        return length;
    }

    public Integer getDecimalPlaces() {
        return decimalPlaces;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public boolean isOrdinal() {
        return ordinal;
    }

    public Column getColumn() {
        Column c = new Column();
        c.setName(getName());
        c.setAutoIncrement(isId());
        if (length != null && (decimalPlaces == null || decimalPlaces == 0)) {
            c.setLength(length);
        }
        if (decimalPlaces != null) {
            c.setPrecision(length);
            c.setScale(decimalPlaces);
        }
        c.setNullable(isNullable());
        if (defaultValue != null && !isId()) {
            c.setDefaultValue(String.valueOf(convertToDB(defaultValue)));
        }
        c.setType(getColumnType());
        return c;
    }

    public boolean isId() {
        return "id".equals(getName());
    }

    private int getColumnType() {
        if (field.getType() == One.class) {
            return Types.INTEGER;
        }
        if (field.getType() == int.class || field.getType() == Integer.class) {
            return Types.INTEGER;
        }
        if (field.getType() == long.class || field.getType() == Long.class) {
            return Types.INTEGER;
        }
        if (field.getType() == double.class || field.getType() == Double.class) {
            return Types.DOUBLE;
        }
        if (field.getType() == float.class || field.getType() == Float.class) {
            return Types.FLOAT;
        }
        if (field.getType() == boolean.class || field.getType() == Boolean.class) {
            return Types.INTEGER;
        }
        if (field.getType() == String.class) {
            return Types.CHAR;
        }
        if (field.getType().isEnum()) {
            if (ordinal) {
                return Types.INTEGER;
            } else {
                return Types.CHAR;
            }
        }
        return 0;
    }

    public Field getField() {
        return field;
    }

    public boolean isReference() {
        return field.getType() == One.class && field.isAnnotationPresent(RefType.class) && Strings.isEmpty(field.getAnnotation(
                RefType.class).field());
    }

    public Class<? extends Entity> getReference() {
        return field.getAnnotation(RefType.class).value();
    }

    public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
        return field.isAnnotationPresent(annotationType);
    }

    public void write(Object instance, Object o) {
        try {
            field.set(instance, o);
        } catch (Throwable e) {
            throw Exceptions.handle()
                            .to(OMA.LOG)
                            .error(e)
                            .withSystemErrorMessage("Cannot access property %s of %s: %s (%s)",
                                                    field.getName(),
                                                    field.getDeclaringClass().getName())
                            .handle();
        }
    }

    public Object read(Object instance) {
        try {
            return field.get(instance);
        } catch (Throwable e) {
            throw Exceptions.handle()
                            .to(OMA.LOG)
                            .error(e)
                            .withSystemErrorMessage("Cannot access property %s of %s: %s (%s)",
                                                    field.getName(),
                                                    field.getDeclaringClass().getName())
                            .handle();
        }
    }

    public String getLabel() {
        return NLS.get(field.getDeclaringClass().getSimpleName() + "." + getName());
    }
}
