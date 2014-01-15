/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.app.oma.schema;

import sirius.kernel.commons.Strings;

/**
 * Provides a large set of reserved SQL keywords based on https://drupal.org/node/141051
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/01
 */
public class SQLKeywords {

    public static boolean isReserved(String key) {
        if (Strings.isEmpty(key)) {
            return false;
        }

        key = key.toUpperCase();

        if ("A".equals(key)) {
            return true;
        }
        if ("ABORT".equals(key)) {
            return true;
        }
        if ("ABS".equals(key)) {
            return true;
        }
        if ("ABSOLUTE".equals(key)) {
            return true;
        }
        if ("ACCESS".equals(key)) {
            return true;
        }
        if ("ACTION".equals(key)) {
            return true;
        }
        if ("ADA".equals(key)) {
            return true;
        }
        if ("ADD".equals(key)) {
            return true;
        }
        if ("ADMIN".equals(key)) {
            return true;
        }
        if ("AFTER".equals(key)) {
            return true;
        }
        if ("AGGREGATE".equals(key)) {
            return true;
        }
        if ("ALIAS".equals(key)) {
            return true;
        }
        if ("ALL".equals(key)) {
            return true;
        }
        if ("ALLOCATE".equals(key)) {
            return true;
        }
        if ("ALSO".equals(key)) {
            return true;
        }
        if ("ALTER".equals(key)) {
            return true;
        }
        if ("ALWAYS".equals(key)) {
            return true;
        }
        if ("ANALYSE".equals(key)) {
            return true;
        }
        if ("ANALYZE".equals(key)) {
            return true;
        }
        if ("AND".equals(key)) {
            return true;
        }
        if ("ANY".equals(key)) {
            return true;
        }
        if ("ARE".equals(key)) {
            return true;
        }
        if ("ARRAY".equals(key)) {
            return true;
        }
        if ("AS".equals(key)) {
            return true;
        }
        if ("ASC".equals(key)) {
            return true;
        }
        if ("ASENSITIVE".equals(key)) {
            return true;
        }
        if ("ASSERTION".equals(key)) {
            return true;
        }
        if ("ASSIGNMENT".equals(key)) {
            return true;
        }
        if ("ASYMMETRIC".equals(key)) {
            return true;
        }
        if ("AT".equals(key)) {
            return true;
        }
        if ("ATOMIC".equals(key)) {
            return true;
        }
        if ("ATTRIBUTE".equals(key)) {
            return true;
        }
        if ("ATTRIBUTES".equals(key)) {
            return true;
        }
        if ("AUDIT".equals(key)) {
            return true;
        }
        if ("AUTHORIZATION".equals(key)) {
            return true;
        }
        if ("AUTO_INCREMENT".equals(key)) {
            return true;
        }
        if ("AVG".equals(key)) {
            return true;
        }
        if ("AVG_ROW_LENGTH".equals(key)) {
            return true;
        }
        if ("BACKUP".equals(key)) {
            return true;
        }
        if ("BACKWARD".equals(key)) {
            return true;
        }
        if ("BEFORE".equals(key)) {
            return true;
        }
        if ("BEGIN".equals(key)) {
            return true;
        }
        if ("BERNOULLI".equals(key)) {
            return true;
        }
        if ("BETWEEN".equals(key)) {
            return true;
        }
        if ("BIGINT".equals(key)) {
            return true;
        }
        if ("BINARY".equals(key)) {
            return true;
        }
        if ("BIT".equals(key)) {
            return true;
        }
        if ("BIT_LENGTH".equals(key)) {
            return true;
        }
        if ("BITVAR".equals(key)) {
            return true;
        }
        if ("BLOB".equals(key)) {
            return true;
        }
        if ("BOOL".equals(key)) {
            return true;
        }
        if ("BOOLEAN".equals(key)) {
            return true;
        }
        if ("BOTH".equals(key)) {
            return true;
        }
        if ("BREADTH".equals(key)) {
            return true;
        }
        if ("BREAK".equals(key)) {
            return true;
        }
        if ("BROWSE".equals(key)) {
            return true;
        }
        if ("BULK".equals(key)) {
            return true;
        }
        if ("BY".equals(key)) {
            return true;
        }
        if ("C".equals(key)) {
            return true;
        }
        if ("CACHE".equals(key)) {
            return true;
        }
        if ("CALL".equals(key)) {
            return true;
        }
        if ("CALLED".equals(key)) {
            return true;
        }
        if ("CARDINALITY".equals(key)) {
            return true;
        }
        if ("CASCADE".equals(key)) {
            return true;
        }
        if ("CASCADED".equals(key)) {
            return true;
        }
        if ("CASE".equals(key)) {
            return true;
        }
        if ("CAST".equals(key)) {
            return true;
        }
        if ("CATALOG".equals(key)) {
            return true;
        }
        if ("CATALOG_NAME".equals(key)) {
            return true;
        }
        if ("CEIL".equals(key)) {
            return true;
        }
        if ("CEILING".equals(key)) {
            return true;
        }
        if ("CHAIN".equals(key)) {
            return true;
        }
        if ("CHANGE".equals(key)) {
            return true;
        }
        if ("CHAR".equals(key)) {
            return true;
        }
        if ("CHAR_LENGTH".equals(key)) {
            return true;
        }
        if ("CHARACTER".equals(key)) {
            return true;
        }
        if ("CHARACTER_LENGTH".equals(key)) {
            return true;
        }
        if ("CHARACTER_SET_CATALOG".equals(key)) {
            return true;
        }
        if ("CHARACTER_SET_NAME".equals(key)) {
            return true;
        }
        if ("CHARACTER_SET_SCHEMA".equals(key)) {
            return true;
        }
        if ("CHARACTERISTICS".equals(key)) {
            return true;
        }
        if ("CHARACTERS".equals(key)) {
            return true;
        }
        if ("CHECK".equals(key)) {
            return true;
        }
        if ("CHECKED".equals(key)) {
            return true;
        }
        if ("CHECKPOINT".equals(key)) {
            return true;
        }
        if ("CHECKSUM".equals(key)) {
            return true;
        }
        if ("CLASS".equals(key)) {
            return true;
        }
        if ("CLASS_ORIGIN".equals(key)) {
            return true;
        }
        if ("CLOB".equals(key)) {
            return true;
        }
        if ("CLOSE".equals(key)) {
            return true;
        }
        if ("CLUSTER".equals(key)) {
            return true;
        }
        if ("CLUSTERED".equals(key)) {
            return true;
        }
        if ("COALESCE".equals(key)) {
            return true;
        }
        if ("COBOL".equals(key)) {
            return true;
        }
        if ("COLLATE".equals(key)) {
            return true;
        }
        if ("COLLATION".equals(key)) {
            return true;
        }
        if ("COLLATION_CATALOG".equals(key)) {
            return true;
        }
        if ("COLLATION_NAME".equals(key)) {
            return true;
        }
        if ("COLLATION_SCHEMA".equals(key)) {
            return true;
        }
        if ("COLLECT".equals(key)) {
            return true;
        }
        if ("COLUMN".equals(key)) {
            return true;
        }
        if ("COLUMN_NAME".equals(key)) {
            return true;
        }
        if ("COLUMNS".equals(key)) {
            return true;
        }
        if ("COMMAND_FUNCTION".equals(key)) {
            return true;
        }
        if ("COMMAND_FUNCTION_CODE".equals(key)) {
            return true;
        }
        if ("COMMENT".equals(key)) {
            return true;
        }
        if ("COMMIT".equals(key)) {
            return true;
        }
        if ("COMMITTED".equals(key)) {
            return true;
        }
        if ("COMPLETION".equals(key)) {
            return true;
        }
        if ("COMPRESS".equals(key)) {
            return true;
        }
        if ("COMPUTE".equals(key)) {
            return true;
        }
        if ("CONDITION".equals(key)) {
            return true;
        }
        if ("CONDITION_NUMBER".equals(key)) {
            return true;
        }
        if ("CONNECT".equals(key)) {
            return true;
        }
        if ("CONNECTION".equals(key)) {
            return true;
        }
        if ("CONNECTION_NAME".equals(key)) {
            return true;
        }
        if ("CONSTRAINT".equals(key)) {
            return true;
        }
        if ("CONSTRAINT_CATALOG".equals(key)) {
            return true;
        }
        if ("CONSTRAINT_NAME".equals(key)) {
            return true;
        }
        if ("CONSTRAINT_SCHEMA".equals(key)) {
            return true;
        }
        if ("CONSTRAINTS".equals(key)) {
            return true;
        }
        if ("CONSTRUCTOR".equals(key)) {
            return true;
        }
        if ("CONTAINS".equals(key)) {
            return true;
        }
        if ("CONTAINSTABLE".equals(key)) {
            return true;
        }
        if ("CONTINUE".equals(key)) {
            return true;
        }
        if ("CONVERSION".equals(key)) {
            return true;
        }
        if ("CONVERT".equals(key)) {
            return true;
        }
        if ("COPY".equals(key)) {
            return true;
        }
        if ("CORR".equals(key)) {
            return true;
        }
        if ("CORRESPONDING".equals(key)) {
            return true;
        }
        if ("COUNT".equals(key)) {
            return true;
        }
        if ("COVAR_POP".equals(key)) {
            return true;
        }
        if ("COVAR_SAMP".equals(key)) {
            return true;
        }
        if ("CREATE".equals(key)) {
            return true;
        }
        if ("CREATEDB".equals(key)) {
            return true;
        }
        if ("CREATEROLE".equals(key)) {
            return true;
        }
        if ("CREATEUSER".equals(key)) {
            return true;
        }
        if ("CROSS".equals(key)) {
            return true;
        }
        if ("CSV".equals(key)) {
            return true;
        }
        if ("CUBE".equals(key)) {
            return true;
        }
        if ("CUME_DIST".equals(key)) {
            return true;
        }
        if ("CURRENT".equals(key)) {
            return true;
        }
        if ("CURRENT_DATE".equals(key)) {
            return true;
        }
        if ("CURRENT_DEFAULT_TRANSFORM_GROUP".equals(key)) {
            return true;
        }
        if ("CURRENT_PATH".equals(key)) {
            return true;
        }
        if ("CURRENT_ROLE".equals(key)) {
            return true;
        }
        if ("CURRENT_TIME".equals(key)) {
            return true;
        }
        if ("CURRENT_TIMESTAMP".equals(key)) {
            return true;
        }
        if ("CURRENT_TRANSFORM_GROUP_FOR_TYPE".equals(key)) {
            return true;
        }
        if ("CURRENT_USER".equals(key)) {
            return true;
        }
        if ("CURSOR".equals(key)) {
            return true;
        }
        if ("CURSOR_NAME".equals(key)) {
            return true;
        }
        if ("CYCLE".equals(key)) {
            return true;
        }
        if ("DATA".equals(key)) {
            return true;
        }
        if ("DATABASE".equals(key)) {
            return true;
        }
        if ("DATABASES".equals(key)) {
            return true;
        }
        if ("DATE".equals(key)) {
            return true;
        }
        if ("DATETIME".equals(key)) {
            return true;
        }
        if ("DATETIME_INTERVAL_CODE".equals(key)) {
            return true;
        }
        if ("DATETIME_INTERVAL_PRECISION".equals(key)) {
            return true;
        }
        if ("DAY".equals(key)) {
            return true;
        }
        if ("DAY_HOUR".equals(key)) {
            return true;
        }
        if ("DAY_MICROSECOND".equals(key)) {
            return true;
        }
        if ("DAY_MINUTE".equals(key)) {
            return true;
        }
        if ("DAY_SECOND".equals(key)) {
            return true;
        }
        if ("DAYOFMONTH".equals(key)) {
            return true;
        }
        if ("DAYOFWEEK".equals(key)) {
            return true;
        }
        if ("DAYOFYEAR".equals(key)) {
            return true;
        }
        if ("DBCC".equals(key)) {
            return true;
        }
        if ("DEALLOCATE".equals(key)) {
            return true;
        }
        if ("DEC".equals(key)) {
            return true;
        }
        if ("DECIMAL".equals(key)) {
            return true;
        }
        if ("DECLARE".equals(key)) {
            return true;
        }
        if ("DEFAULT".equals(key)) {
            return true;
        }
        if ("DEFAULTS".equals(key)) {
            return true;
        }
        if ("DEFERRABLE".equals(key)) {
            return true;
        }
        if ("DEFERRED".equals(key)) {
            return true;
        }
        if ("DEFINED".equals(key)) {
            return true;
        }
        if ("DEFINER".equals(key)) {
            return true;
        }
        if ("DEGREE".equals(key)) {
            return true;
        }
        if ("DELAY_KEY_WRITE".equals(key)) {
            return true;
        }
        if ("DELAYED".equals(key)) {
            return true;
        }
        if ("DELETE".equals(key)) {
            return true;
        }
        if ("DELIMITER".equals(key)) {
            return true;
        }
        if ("DELIMITERS".equals(key)) {
            return true;
        }
        if ("DENSE_RANK".equals(key)) {
            return true;
        }
        if ("DENY".equals(key)) {
            return true;
        }
        if ("DEPTH".equals(key)) {
            return true;
        }
        if ("DEREF".equals(key)) {
            return true;
        }
        if ("DERIVED".equals(key)) {
            return true;
        }
        if ("DESC".equals(key)) {
            return true;
        }
        if ("DESCRIBE".equals(key)) {
            return true;
        }
        if ("DESCRIPTOR".equals(key)) {
            return true;
        }
        if ("DESTROY".equals(key)) {
            return true;
        }
        if ("DESTRUCTOR".equals(key)) {
            return true;
        }
        if ("DETERMINISTIC".equals(key)) {
            return true;
        }
        if ("DIAGNOSTICS".equals(key)) {
            return true;
        }
        if ("DICTIONARY".equals(key)) {
            return true;
        }
        if ("DISABLE".equals(key)) {
            return true;
        }
        if ("DISCONNECT".equals(key)) {
            return true;
        }
        if ("DISK".equals(key)) {
            return true;
        }
        if ("DISPATCH".equals(key)) {
            return true;
        }
        if ("DISTINCT".equals(key)) {
            return true;
        }
        if ("DISTINCTROW".equals(key)) {
            return true;
        }
        if ("DISTRIBUTED".equals(key)) {
            return true;
        }
        if ("DIV".equals(key)) {
            return true;
        }
        if ("DO".equals(key)) {
            return true;
        }
        if ("DOMAIN".equals(key)) {
            return true;
        }
        if ("DOUBLE".equals(key)) {
            return true;
        }
        if ("DROP".equals(key)) {
            return true;
        }
        if ("DUAL".equals(key)) {
            return true;
        }
        if ("DUMMY".equals(key)) {
            return true;
        }
        if ("DUMP".equals(key)) {
            return true;
        }
        if ("DYNAMIC".equals(key)) {
            return true;
        }
        if ("DYNAMIC_FUNCTION".equals(key)) {
            return true;
        }
        if ("DYNAMIC_FUNCTION_CODE".equals(key)) {
            return true;
        }
        if ("EACH".equals(key)) {
            return true;
        }
        if ("ELEMENT".equals(key)) {
            return true;
        }
        if ("ELSE".equals(key)) {
            return true;
        }
        if ("ELSEIF".equals(key)) {
            return true;
        }
        if ("ENABLE".equals(key)) {
            return true;
        }
        if ("ENCLOSED".equals(key)) {
            return true;
        }
        if ("ENCODING".equals(key)) {
            return true;
        }
        if ("ENCRYPTED".equals(key)) {
            return true;
        }
        if ("END".equals(key)) {
            return true;
        }
        if ("END-EXEC".equals(key)) {
            return true;
        }
        if ("ENUM".equals(key)) {
            return true;
        }
        if ("EQUALS".equals(key)) {
            return true;
        }
        if ("ERRLVL".equals(key)) {
            return true;
        }
        if ("ESCAPE".equals(key)) {
            return true;
        }
        if ("ESCAPED".equals(key)) {
            return true;
        }
        if ("EVERY".equals(key)) {
            return true;
        }
        if ("EXCEPT".equals(key)) {
            return true;
        }
        if ("EXCEPTION".equals(key)) {
            return true;
        }
        if ("EXCLUDE".equals(key)) {
            return true;
        }
        if ("EXCLUDING".equals(key)) {
            return true;
        }
        if ("EXCLUSIVE".equals(key)) {
            return true;
        }
        if ("EXEC".equals(key)) {
            return true;
        }
        if ("EXECUTE".equals(key)) {
            return true;
        }
        if ("EXISTING".equals(key)) {
            return true;
        }
        if ("EXISTS".equals(key)) {
            return true;
        }
        if ("EXIT".equals(key)) {
            return true;
        }
        if ("EXP".equals(key)) {
            return true;
        }
        if ("EXPLAIN".equals(key)) {
            return true;
        }
        if ("EXTERNAL".equals(key)) {
            return true;
        }
        if ("EXTRACT".equals(key)) {
            return true;
        }
        if ("FALSE".equals(key)) {
            return true;
        }
        if ("FETCH".equals(key)) {
            return true;
        }
        if ("FIELDS".equals(key)) {
            return true;
        }
        if ("FILE".equals(key)) {
            return true;
        }
        if ("FILLFACTOR".equals(key)) {
            return true;
        }
        if ("FILTER".equals(key)) {
            return true;
        }
        if ("FINAL".equals(key)) {
            return true;
        }
        if ("FIRST".equals(key)) {
            return true;
        }
        if ("FLOAT".equals(key)) {
            return true;
        }
        if ("FLOAT4".equals(key)) {
            return true;
        }
        if ("FLOAT8".equals(key)) {
            return true;
        }
        if ("FLOOR".equals(key)) {
            return true;
        }
        if ("FLUSH".equals(key)) {
            return true;
        }
        if ("FOLLOWING".equals(key)) {
            return true;
        }
        if ("FOR".equals(key)) {
            return true;
        }
        if ("FORCE".equals(key)) {
            return true;
        }
        if ("FOREIGN".equals(key)) {
            return true;
        }
        if ("FORTRAN".equals(key)) {
            return true;
        }
        if ("FORWARD".equals(key)) {
            return true;
        }
        if ("FOUND".equals(key)) {
            return true;
        }
        if ("FREE".equals(key)) {
            return true;
        }
        if ("FREETEXT".equals(key)) {
            return true;
        }
        if ("FREETEXTTABLE".equals(key)) {
            return true;
        }
        if ("FREEZE".equals(key)) {
            return true;
        }
        if ("FROM".equals(key)) {
            return true;
        }
        if ("FULL".equals(key)) {
            return true;
        }
        if ("FULLTEXT".equals(key)) {
            return true;
        }
        if ("FUNCTION".equals(key)) {
            return true;
        }
        if ("FUSION".equals(key)) {
            return true;
        }
        if ("G".equals(key)) {
            return true;
        }
        if ("GENERAL".equals(key)) {
            return true;
        }
        if ("GENERATED".equals(key)) {
            return true;
        }
        if ("GET".equals(key)) {
            return true;
        }
        if ("GLOBAL".equals(key)) {
            return true;
        }
        if ("GO".equals(key)) {
            return true;
        }
        if ("GOTO".equals(key)) {
            return true;
        }
        if ("GRANT".equals(key)) {
            return true;
        }
        if ("GRANTED".equals(key)) {
            return true;
        }
        if ("GRANTS".equals(key)) {
            return true;
        }
        if ("GREATEST".equals(key)) {
            return true;
        }
        if ("GROUP".equals(key)) {
            return true;
        }
        if ("GROUPING".equals(key)) {
            return true;
        }
        if ("HANDLER".equals(key)) {
            return true;
        }
        if ("HAVING".equals(key)) {
            return true;
        }
        if ("HEADER".equals(key)) {
            return true;
        }
        if ("HEAP".equals(key)) {
            return true;
        }
        if ("HIERARCHY".equals(key)) {
            return true;
        }
        if ("HIGH_PRIORITY".equals(key)) {
            return true;
        }
        if ("HOLD".equals(key)) {
            return true;
        }
        if ("HOLDLOCK".equals(key)) {
            return true;
        }
        if ("HOST".equals(key)) {
            return true;
        }
        if ("HOSTS".equals(key)) {
            return true;
        }
        if ("HOUR".equals(key)) {
            return true;
        }
        if ("HOUR_MICROSECOND".equals(key)) {
            return true;
        }
        if ("HOUR_MINUTE".equals(key)) {
            return true;
        }
        if ("HOUR_SECOND".equals(key)) {
            return true;
        }
        if ("IDENTIFIED".equals(key)) {
            return true;
        }
        if ("IDENTITY".equals(key)) {
            return true;
        }
        if ("IDENTITY_INSERT".equals(key)) {
            return true;
        }
        if ("IDENTITYCOL".equals(key)) {
            return true;
        }
        if ("IF".equals(key)) {
            return true;
        }
        if ("IGNORE".equals(key)) {
            return true;
        }
        if ("ILIKE".equals(key)) {
            return true;
        }
        if ("IMMEDIATE".equals(key)) {
            return true;
        }
        if ("IMMUTABLE".equals(key)) {
            return true;
        }
        if ("IMPLEMENTATION".equals(key)) {
            return true;
        }
        if ("IMPLICIT".equals(key)) {
            return true;
        }
        if ("IN".equals(key)) {
            return true;
        }
        if ("INCLUDE".equals(key)) {
            return true;
        }
        if ("INCLUDING".equals(key)) {
            return true;
        }
        if ("INCREMENT".equals(key)) {
            return true;
        }
        if ("INDEX".equals(key)) {
            return true;
        }
        if ("INDICATOR".equals(key)) {
            return true;
        }
        if ("INFILE".equals(key)) {
            return true;
        }
        if ("INFIX".equals(key)) {
            return true;
        }
        if ("INHERIT".equals(key)) {
            return true;
        }
        if ("INHERITS".equals(key)) {
            return true;
        }
        if ("INITIAL".equals(key)) {
            return true;
        }
        if ("INITIALIZE".equals(key)) {
            return true;
        }
        if ("INITIALLY".equals(key)) {
            return true;
        }
        if ("INNER".equals(key)) {
            return true;
        }
        if ("INOUT".equals(key)) {
            return true;
        }
        if ("INPUT".equals(key)) {
            return true;
        }
        if ("INSENSITIVE".equals(key)) {
            return true;
        }
        if ("INSERT".equals(key)) {
            return true;
        }
        if ("INSERT_ID".equals(key)) {
            return true;
        }
        if ("INSTANCE".equals(key)) {
            return true;
        }
        if ("INSTANTIABLE".equals(key)) {
            return true;
        }
        if ("INSTEAD".equals(key)) {
            return true;
        }
        if ("INT".equals(key)) {
            return true;
        }
        if ("INT1".equals(key)) {
            return true;
        }
        if ("INT2".equals(key)) {
            return true;
        }
        if ("INT3".equals(key)) {
            return true;
        }
        if ("INT4".equals(key)) {
            return true;
        }
        if ("INT8".equals(key)) {
            return true;
        }
        if ("INTEGER".equals(key)) {
            return true;
        }
        if ("INTERSECT".equals(key)) {
            return true;
        }
        if ("INTERSECTION".equals(key)) {
            return true;
        }
        if ("INTERVAL".equals(key)) {
            return true;
        }
        if ("INTO".equals(key)) {
            return true;
        }
        if ("INVOKER".equals(key)) {
            return true;
        }
        if ("IS".equals(key)) {
            return true;
        }
        if ("ISAM".equals(key)) {
            return true;
        }
        if ("ISNULL".equals(key)) {
            return true;
        }
        if ("ISOLATION".equals(key)) {
            return true;
        }
        if ("ITERATE".equals(key)) {
            return true;
        }
        if ("JOIN".equals(key)) {
            return true;
        }
        if ("K".equals(key)) {
            return true;
        }
        if ("KEY".equals(key)) {
            return true;
        }
        if ("KEY_MEMBER".equals(key)) {
            return true;
        }
        if ("KEY_TYPE".equals(key)) {
            return true;
        }
        if ("KEYS".equals(key)) {
            return true;
        }
        if ("KILL".equals(key)) {
            return true;
        }
        if ("LANCOMPILER".equals(key)) {
            return true;
        }
        if ("LANGUAGE".equals(key)) {
            return true;
        }
        if ("LARGE".equals(key)) {
            return true;
        }
        if ("LAST".equals(key)) {
            return true;
        }
        if ("LAST_INSERT_ID".equals(key)) {
            return true;
        }
        if ("LATERAL".equals(key)) {
            return true;
        }
        if ("LEADING".equals(key)) {
            return true;
        }
        if ("LEAST".equals(key)) {
            return true;
        }
        if ("LEAVE".equals(key)) {
            return true;
        }
        if ("LEFT".equals(key)) {
            return true;
        }
        if ("LENGTH".equals(key)) {
            return true;
        }
        if ("LESS".equals(key)) {
            return true;
        }
        if ("LEVEL".equals(key)) {
            return true;
        }
        if ("LIKE".equals(key)) {
            return true;
        }
        if ("LIMIT".equals(key)) {
            return true;
        }
        if ("LINENO".equals(key)) {
            return true;
        }
        if ("LINES".equals(key)) {
            return true;
        }
        if ("LISTEN".equals(key)) {
            return true;
        }
        if ("LN".equals(key)) {
            return true;
        }
        if ("LOAD".equals(key)) {
            return true;
        }
        if ("LOCAL".equals(key)) {
            return true;
        }
        if ("LOCALTIME".equals(key)) {
            return true;
        }
        if ("LOCALTIMESTAMP".equals(key)) {
            return true;
        }
        if ("LOCATION".equals(key)) {
            return true;
        }
        if ("LOCATOR".equals(key)) {
            return true;
        }
        if ("LOCK".equals(key)) {
            return true;
        }
        if ("LOGIN".equals(key)) {
            return true;
        }
        if ("LOGS".equals(key)) {
            return true;
        }
        if ("LONG".equals(key)) {
            return true;
        }
        if ("LONGBLOB".equals(key)) {
            return true;
        }
        if ("LONGTEXT".equals(key)) {
            return true;
        }
        if ("LOOP".equals(key)) {
            return true;
        }
        if ("LOW_PRIORITY".equals(key)) {
            return true;
        }
        if ("LOWER".equals(key)) {
            return true;
        }
        if ("M".equals(key)) {
            return true;
        }
        if ("MAP".equals(key)) {
            return true;
        }
        if ("MATCH".equals(key)) {
            return true;
        }
        if ("MATCHED".equals(key)) {
            return true;
        }
        if ("MAX".equals(key)) {
            return true;
        }
        if ("MAX_ROWS".equals(key)) {
            return true;
        }
        if ("MAXEXTENTS".equals(key)) {
            return true;
        }
        if ("MAXVALUE".equals(key)) {
            return true;
        }
        if ("MEDIUMBLOB".equals(key)) {
            return true;
        }
        if ("MEDIUMINT".equals(key)) {
            return true;
        }
        if ("MEDIUMTEXT".equals(key)) {
            return true;
        }
        if ("MEMBER".equals(key)) {
            return true;
        }
        if ("MERGE".equals(key)) {
            return true;
        }
        if ("MESSAGE_LENGTH".equals(key)) {
            return true;
        }
        if ("MESSAGE_OCTET_LENGTH".equals(key)) {
            return true;
        }
        if ("MESSAGE_TEXT".equals(key)) {
            return true;
        }
        if ("METHOD".equals(key)) {
            return true;
        }
        if ("MIDDLEINT".equals(key)) {
            return true;
        }
        if ("MIN".equals(key)) {
            return true;
        }
        if ("MIN_ROWS".equals(key)) {
            return true;
        }
        if ("MINUS".equals(key)) {
            return true;
        }
        if ("MINUTE".equals(key)) {
            return true;
        }
        if ("MINUTE_MICROSECOND".equals(key)) {
            return true;
        }
        if ("MINUTE_SECOND".equals(key)) {
            return true;
        }
        if ("MINVALUE".equals(key)) {
            return true;
        }
        if ("MLSLABEL".equals(key)) {
            return true;
        }
        if ("MOD".equals(key)) {
            return true;
        }
        if ("MODE".equals(key)) {
            return true;
        }
        if ("MODIFIES".equals(key)) {
            return true;
        }
        if ("MODIFY".equals(key)) {
            return true;
        }
        if ("MODULE".equals(key)) {
            return true;
        }
        if ("MONTH".equals(key)) {
            return true;
        }
        if ("MONTHNAME".equals(key)) {
            return true;
        }
        if ("MORE".equals(key)) {
            return true;
        }
        if ("MOVE".equals(key)) {
            return true;
        }
        if ("MULTISET".equals(key)) {
            return true;
        }
        if ("MUMPS".equals(key)) {
            return true;
        }
        if ("MYISAM".equals(key)) {
            return true;
        }
        if ("NAMES".equals(key)) {
            return true;
        }
        if ("NATIONAL".equals(key)) {
            return true;
        }
        if ("NATURAL".equals(key)) {
            return true;
        }
        if ("NCHAR".equals(key)) {
            return true;
        }
        if ("NCLOB".equals(key)) {
            return true;
        }
        if ("NESTING".equals(key)) {
            return true;
        }
        if ("NEW".equals(key)) {
            return true;
        }
        if ("NEXT".equals(key)) {
            return true;
        }
        if ("NO".equals(key)) {
            return true;
        }
        if ("NO_WRITE_TO_BINLOG".equals(key)) {
            return true;
        }
        if ("NOAUDIT".equals(key)) {
            return true;
        }
        if ("NOCHECK".equals(key)) {
            return true;
        }
        if ("NOCOMPRESS".equals(key)) {
            return true;
        }
        if ("NOCREATEDB".equals(key)) {
            return true;
        }
        if ("NOCREATEROLE".equals(key)) {
            return true;
        }
        if ("NOCREATEUSER".equals(key)) {
            return true;
        }
        if ("NOINHERIT".equals(key)) {
            return true;
        }
        if ("NOLOGIN".equals(key)) {
            return true;
        }
        if ("NONCLUSTERED".equals(key)) {
            return true;
        }
        if ("NONE".equals(key)) {
            return true;
        }
        if ("NORMALIZE".equals(key)) {
            return true;
        }
        if ("NORMALIZED".equals(key)) {
            return true;
        }
        if ("NOSUPERUSER".equals(key)) {
            return true;
        }
        if ("NOT".equals(key)) {
            return true;
        }
        if ("NOTHING".equals(key)) {
            return true;
        }
        if ("NOTIFY".equals(key)) {
            return true;
        }
        if ("NOTNULL".equals(key)) {
            return true;
        }
        if ("NOWAIT".equals(key)) {
            return true;
        }
        if ("NULL".equals(key)) {
            return true;
        }
        if ("NULLABLE".equals(key)) {
            return true;
        }
        if ("NULLIF".equals(key)) {
            return true;
        }
        if ("NULLS".equals(key)) {
            return true;
        }
        if ("NUMBER".equals(key)) {
            return true;
        }
        if ("NUMERIC".equals(key)) {
            return true;
        }
        if ("OBJECT".equals(key)) {
            return true;
        }
        if ("OCTET_LENGTH".equals(key)) {
            return true;
        }
        if ("OCTETS".equals(key)) {
            return true;
        }
        if ("OF".equals(key)) {
            return true;
        }
        if ("OFF".equals(key)) {
            return true;
        }
        if ("OFFLINE".equals(key)) {
            return true;
        }
        if ("OFFSET".equals(key)) {
            return true;
        }
        if ("OFFSETS".equals(key)) {
            return true;
        }
        if ("OIDS".equals(key)) {
            return true;
        }
        if ("OLD".equals(key)) {
            return true;
        }
        if ("ON".equals(key)) {
            return true;
        }
        if ("ONLINE".equals(key)) {
            return true;
        }
        if ("ONLY".equals(key)) {
            return true;
        }
        if ("OPEN".equals(key)) {
            return true;
        }
        if ("OPENDATASOURCE".equals(key)) {
            return true;
        }
        if ("OPENQUERY".equals(key)) {
            return true;
        }
        if ("OPENROWSET".equals(key)) {
            return true;
        }
        if ("OPENXML".equals(key)) {
            return true;
        }
        if ("OPERATION".equals(key)) {
            return true;
        }
        if ("OPERATOR".equals(key)) {
            return true;
        }
        if ("OPTIMIZE".equals(key)) {
            return true;
        }
        if ("OPTION".equals(key)) {
            return true;
        }
        if ("OPTIONALLY".equals(key)) {
            return true;
        }
        if ("OPTIONS".equals(key)) {
            return true;
        }
        if ("OR".equals(key)) {
            return true;
        }
        if ("ORDER".equals(key)) {
            return true;
        }
        if ("ORDERING".equals(key)) {
            return true;
        }
        if ("ORDINALITY".equals(key)) {
            return true;
        }
        if ("OTHERS".equals(key)) {
            return true;
        }
        if ("OUT".equals(key)) {
            return true;
        }
        if ("OUTER".equals(key)) {
            return true;
        }
        if ("OUTFILE".equals(key)) {
            return true;
        }
        if ("OUTPUT".equals(key)) {
            return true;
        }
        if ("OVER".equals(key)) {
            return true;
        }
        if ("OVERLAPS".equals(key)) {
            return true;
        }
        if ("OVERLAY".equals(key)) {
            return true;
        }
        if ("OVERRIDING".equals(key)) {
            return true;
        }
        if ("OWNER".equals(key)) {
            return true;
        }
        if ("PACK_KEYS".equals(key)) {
            return true;
        }
        if ("PAD".equals(key)) {
            return true;
        }
        if ("PARAMETER".equals(key)) {
            return true;
        }
        if ("PARAMETER_MODE".equals(key)) {
            return true;
        }
        if ("PARAMETER_NAME".equals(key)) {
            return true;
        }
        if ("PARAMETER_ORDINAL_POSITION".equals(key)) {
            return true;
        }
        if ("PARAMETER_SPECIFIC_CATALOG".equals(key)) {
            return true;
        }
        if ("PARAMETER_SPECIFIC_NAME".equals(key)) {
            return true;
        }
        if ("PARAMETER_SPECIFIC_SCHEMA".equals(key)) {
            return true;
        }
        if ("PARAMETERS".equals(key)) {
            return true;
        }
        if ("PARTIAL".equals(key)) {
            return true;
        }
        if ("PARTITION".equals(key)) {
            return true;
        }
        if ("PASCAL".equals(key)) {
            return true;
        }
        if ("PASSWORD".equals(key)) {
            return true;
        }
        if ("PATH".equals(key)) {
            return true;
        }
        if ("PCTFREE".equals(key)) {
            return true;
        }
        if ("PERCENT".equals(key)) {
            return true;
        }
        if ("PERCENT_RANK".equals(key)) {
            return true;
        }
        if ("PERCENTILE_CONT".equals(key)) {
            return true;
        }
        if ("PERCENTILE_DISC".equals(key)) {
            return true;
        }
        if ("PLACING".equals(key)) {
            return true;
        }
        if ("PLAN".equals(key)) {
            return true;
        }
        if ("PLI".equals(key)) {
            return true;
        }
        if ("POSITION".equals(key)) {
            return true;
        }
        if ("POSTFIX".equals(key)) {
            return true;
        }
        if ("POWER".equals(key)) {
            return true;
        }
        if ("PRECEDING".equals(key)) {
            return true;
        }
        if ("PRECISION".equals(key)) {
            return true;
        }
        if ("PREFIX".equals(key)) {
            return true;
        }
        if ("PREORDER".equals(key)) {
            return true;
        }
        if ("PREPARE".equals(key)) {
            return true;
        }
        if ("PREPARED".equals(key)) {
            return true;
        }
        if ("PRESERVE".equals(key)) {
            return true;
        }
        if ("PRIMARY".equals(key)) {
            return true;
        }
        if ("PRINT".equals(key)) {
            return true;
        }
        if ("PRIOR".equals(key)) {
            return true;
        }
        if ("PRIVILEGES".equals(key)) {
            return true;
        }
        if ("PROC".equals(key)) {
            return true;
        }
        if ("PROCEDURAL".equals(key)) {
            return true;
        }
        if ("PROCEDURE".equals(key)) {
            return true;
        }
        if ("PROCESS".equals(key)) {
            return true;
        }
        if ("PROCESSLIST".equals(key)) {
            return true;
        }
        if ("PUBLIC".equals(key)) {
            return true;
        }
        if ("PURGE".equals(key)) {
            return true;
        }
        if ("QUOTE".equals(key)) {
            return true;
        }
        if ("RAID0".equals(key)) {
            return true;
        }
        if ("RAISERROR".equals(key)) {
            return true;
        }
        if ("RANGE".equals(key)) {
            return true;
        }
        if ("RANK".equals(key)) {
            return true;
        }
        if ("RAW".equals(key)) {
            return true;
        }
        if ("READ".equals(key)) {
            return true;
        }
        if ("READS".equals(key)) {
            return true;
        }
        if ("READTEXT".equals(key)) {
            return true;
        }
        if ("REAL".equals(key)) {
            return true;
        }
        if ("RECHECK".equals(key)) {
            return true;
        }
        if ("RECONFIGURE".equals(key)) {
            return true;
        }
        if ("RECURSIVE".equals(key)) {
            return true;
        }
        if ("REF".equals(key)) {
            return true;
        }
        if ("REFERENCES".equals(key)) {
            return true;
        }
        if ("REFERENCING".equals(key)) {
            return true;
        }
        if ("REGEXP".equals(key)) {
            return true;
        }
        if ("REGR_AVGX".equals(key)) {
            return true;
        }
        if ("REGR_AVGY".equals(key)) {
            return true;
        }
        if ("REGR_COUNT".equals(key)) {
            return true;
        }
        if ("REGR_INTERCEPT".equals(key)) {
            return true;
        }
        if ("REGR_R2".equals(key)) {
            return true;
        }
        if ("REGR_SLOPE".equals(key)) {
            return true;
        }
        if ("REGR_SXX".equals(key)) {
            return true;
        }
        if ("REGR_SXY".equals(key)) {
            return true;
        }
        if ("REGR_SYY".equals(key)) {
            return true;
        }
        if ("REINDEX".equals(key)) {
            return true;
        }
        if ("RELATIVE".equals(key)) {
            return true;
        }
        if ("RELEASE".equals(key)) {
            return true;
        }
        if ("RELOAD".equals(key)) {
            return true;
        }
        if ("RENAME".equals(key)) {
            return true;
        }
        if ("REPEAT".equals(key)) {
            return true;
        }
        if ("REPEATABLE".equals(key)) {
            return true;
        }
        if ("REPLACE".equals(key)) {
            return true;
        }
        if ("REPLICATION".equals(key)) {
            return true;
        }
        if ("REQUIRE".equals(key)) {
            return true;
        }
        if ("RESET".equals(key)) {
            return true;
        }
        if ("RESIGNAL".equals(key)) {
            return true;
        }
        if ("RESOURCE".equals(key)) {
            return true;
        }
        if ("RESTART".equals(key)) {
            return true;
        }
        if ("RESTORE".equals(key)) {
            return true;
        }
        if ("RESTRICT".equals(key)) {
            return true;
        }
        if ("RESULT".equals(key)) {
            return true;
        }
        if ("RETURN".equals(key)) {
            return true;
        }
        if ("RETURNED_CARDINALITY".equals(key)) {
            return true;
        }
        if ("RETURNED_LENGTH".equals(key)) {
            return true;
        }
        if ("RETURNED_OCTET_LENGTH".equals(key)) {
            return true;
        }
        if ("RETURNED_SQLSTATE".equals(key)) {
            return true;
        }
        if ("RETURNS".equals(key)) {
            return true;
        }
        if ("REVOKE".equals(key)) {
            return true;
        }
        if ("RIGHT".equals(key)) {
            return true;
        }
        if ("RLIKE".equals(key)) {
            return true;
        }
        if ("ROLE".equals(key)) {
            return true;
        }
        if ("ROLLBACK".equals(key)) {
            return true;
        }
        if ("ROLLUP".equals(key)) {
            return true;
        }
        if ("ROUTINE".equals(key)) {
            return true;
        }
        if ("ROUTINE_CATALOG".equals(key)) {
            return true;
        }
        if ("ROUTINE_NAME".equals(key)) {
            return true;
        }
        if ("ROUTINE_SCHEMA".equals(key)) {
            return true;
        }
        if ("ROW".equals(key)) {
            return true;
        }
        if ("ROW_COUNT".equals(key)) {
            return true;
        }
        if ("ROW_NUMBER".equals(key)) {
            return true;
        }
        if ("ROWCOUNT".equals(key)) {
            return true;
        }
        if ("ROWGUIDCOL".equals(key)) {
            return true;
        }
        if ("ROWID".equals(key)) {
            return true;
        }
        if ("ROWNUM".equals(key)) {
            return true;
        }
        if ("ROWS".equals(key)) {
            return true;
        }
        if ("RULE".equals(key)) {
            return true;
        }
        if ("SAVE".equals(key)) {
            return true;
        }
        if ("SAVEPOINT".equals(key)) {
            return true;
        }
        if ("SCALE".equals(key)) {
            return true;
        }
        if ("SCHEMA".equals(key)) {
            return true;
        }
        if ("SCHEMA_NAME".equals(key)) {
            return true;
        }
        if ("SCHEMAS".equals(key)) {
            return true;
        }
        if ("SCOPE".equals(key)) {
            return true;
        }
        if ("SCOPE_CATALOG".equals(key)) {
            return true;
        }
        if ("SCOPE_NAME".equals(key)) {
            return true;
        }
        if ("SCOPE_SCHEMA".equals(key)) {
            return true;
        }
        if ("SCROLL".equals(key)) {
            return true;
        }
        if ("SEARCH".equals(key)) {
            return true;
        }
        if ("SECOND".equals(key)) {
            return true;
        }
        if ("SECOND_MICROSECOND".equals(key)) {
            return true;
        }
        if ("SECTION".equals(key)) {
            return true;
        }
        if ("SECURITY".equals(key)) {
            return true;
        }
        if ("SELECT".equals(key)) {
            return true;
        }
        if ("SELF".equals(key)) {
            return true;
        }
        if ("SENSITIVE".equals(key)) {
            return true;
        }
        if ("SEPARATOR".equals(key)) {
            return true;
        }
        if ("SEQUENCE".equals(key)) {
            return true;
        }
        if ("SERIALIZABLE".equals(key)) {
            return true;
        }
        if ("SERVER_NAME".equals(key)) {
            return true;
        }
        if ("SESSION".equals(key)) {
            return true;
        }
        if ("SESSION_USER".equals(key)) {
            return true;
        }
        if ("SET".equals(key)) {
            return true;
        }
        if ("SETOF".equals(key)) {
            return true;
        }
        if ("SETS".equals(key)) {
            return true;
        }
        if ("SETUSER".equals(key)) {
            return true;
        }
        if ("SHARE".equals(key)) {
            return true;
        }
        if ("SHOW".equals(key)) {
            return true;
        }
        if ("SHUTDOWN".equals(key)) {
            return true;
        }
        if ("SIGNAL".equals(key)) {
            return true;
        }
        if ("SIMILAR".equals(key)) {
            return true;
        }
        if ("SIMPLE".equals(key)) {
            return true;
        }
        if ("SIZE".equals(key)) {
            return true;
        }
        if ("SMALLINT".equals(key)) {
            return true;
        }
        if ("SOME".equals(key)) {
            return true;
        }
        if ("SONAME".equals(key)) {
            return true;
        }
        if ("SOURCE".equals(key)) {
            return true;
        }
        if ("SPACE".equals(key)) {
            return true;
        }
        if ("SPATIAL".equals(key)) {
            return true;
        }
        if ("SPECIFIC".equals(key)) {
            return true;
        }
        if ("SPECIFIC_NAME".equals(key)) {
            return true;
        }
        if ("SPECIFICTYPE".equals(key)) {
            return true;
        }
        if ("SQL".equals(key)) {
            return true;
        }
        if ("SQL_BIG_RESULT".equals(key)) {
            return true;
        }
        if ("SQL_BIG_SELECTS".equals(key)) {
            return true;
        }
        if ("SQL_BIG_TABLES".equals(key)) {
            return true;
        }
        if ("SQL_CALC_FOUND_ROWS".equals(key)) {
            return true;
        }
        if ("SQL_LOG_OFF".equals(key)) {
            return true;
        }
        if ("SQL_LOG_UPDATE".equals(key)) {
            return true;
        }
        if ("SQL_LOW_PRIORITY_UPDATES".equals(key)) {
            return true;
        }
        if ("SQL_SELECT_LIMIT".equals(key)) {
            return true;
        }
        if ("SQL_SMALL_RESULT".equals(key)) {
            return true;
        }
        if ("SQL_WARNINGS".equals(key)) {
            return true;
        }
        if ("SQLCA".equals(key)) {
            return true;
        }
        if ("SQLCODE".equals(key)) {
            return true;
        }
        if ("SQLERROR".equals(key)) {
            return true;
        }
        if ("SQLEXCEPTION".equals(key)) {
            return true;
        }
        if ("SQLSTATE".equals(key)) {
            return true;
        }
        if ("SQLWARNING".equals(key)) {
            return true;
        }
        if ("SQRT".equals(key)) {
            return true;
        }
        if ("SSL".equals(key)) {
            return true;
        }
        if ("STABLE".equals(key)) {
            return true;
        }
        if ("START".equals(key)) {
            return true;
        }
        if ("STARTING".equals(key)) {
            return true;
        }
        if ("STATE".equals(key)) {
            return true;
        }
        if ("STATEMENT".equals(key)) {
            return true;
        }
        if ("STATIC".equals(key)) {
            return true;
        }
        if ("STATISTICS".equals(key)) {
            return true;
        }
        if ("STATUS".equals(key)) {
            return true;
        }
        if ("STDDEV_POP".equals(key)) {
            return true;
        }
        if ("STDDEV_SAMP".equals(key)) {
            return true;
        }
        if ("STDIN".equals(key)) {
            return true;
        }
        if ("STDOUT".equals(key)) {
            return true;
        }
        if ("STORAGE".equals(key)) {
            return true;
        }
        if ("STRAIGHT_JOIN".equals(key)) {
            return true;
        }
        if ("STRICT".equals(key)) {
            return true;
        }
        if ("STRING".equals(key)) {
            return true;
        }
        if ("STRUCTURE".equals(key)) {
            return true;
        }
        if ("STYLE".equals(key)) {
            return true;
        }
        if ("SUBCLASS_ORIGIN".equals(key)) {
            return true;
        }
        if ("SUBLIST".equals(key)) {
            return true;
        }
        if ("SUBMULTISET".equals(key)) {
            return true;
        }
        if ("SUBSTRING".equals(key)) {
            return true;
        }
        if ("SUCCESSFUL".equals(key)) {
            return true;
        }
        if ("SUM".equals(key)) {
            return true;
        }
        if ("SUPERUSER".equals(key)) {
            return true;
        }
        if ("SYMMETRIC".equals(key)) {
            return true;
        }
        if ("SYNONYM".equals(key)) {
            return true;
        }
        if ("SYSDATE".equals(key)) {
            return true;
        }
        if ("SYSID".equals(key)) {
            return true;
        }
        if ("SYSTEM".equals(key)) {
            return true;
        }
        if ("SYSTEM_USER".equals(key)) {
            return true;
        }
        if ("TABLE".equals(key)) {
            return true;
        }
        if ("TABLE_NAME".equals(key)) {
            return true;
        }
        if ("TABLES".equals(key)) {
            return true;
        }
        if ("TABLESAMPLE".equals(key)) {
            return true;
        }
        if ("TABLESPACE".equals(key)) {
            return true;
        }
        if ("TEMP".equals(key)) {
            return true;
        }
        if ("TEMPLATE".equals(key)) {
            return true;
        }
        if ("TEMPORARY".equals(key)) {
            return true;
        }
        if ("TERMINATE".equals(key)) {
            return true;
        }
        if ("TERMINATED".equals(key)) {
            return true;
        }
        if ("TEXT".equals(key)) {
            return true;
        }
        if ("TEXTSIZE".equals(key)) {
            return true;
        }
        if ("THAN".equals(key)) {
            return true;
        }
        if ("THEN".equals(key)) {
            return true;
        }
        if ("TIES".equals(key)) {
            return true;
        }
        if ("TIME".equals(key)) {
            return true;
        }
        if ("TIMESTAMP".equals(key)) {
            return true;
        }
        if ("TIMEZONE_HOUR".equals(key)) {
            return true;
        }
        if ("TIMEZONE_MINUTE".equals(key)) {
            return true;
        }
        if ("TINYBLOB".equals(key)) {
            return true;
        }
        if ("TINYINT".equals(key)) {
            return true;
        }
        if ("TINYTEXT".equals(key)) {
            return true;
        }
        if ("TO".equals(key)) {
            return true;
        }
        if ("TOAST".equals(key)) {
            return true;
        }
        if ("TOP".equals(key)) {
            return true;
        }
        if ("TOP_LEVEL_COUNT".equals(key)) {
            return true;
        }
        if ("TRAILING".equals(key)) {
            return true;
        }
        if ("TRAN".equals(key)) {
            return true;
        }
        if ("TRANSACTION".equals(key)) {
            return true;
        }
        if ("TRANSACTION_ACTIVE".equals(key)) {
            return true;
        }
        if ("TRANSACTIONS_COMMITTED".equals(key)) {
            return true;
        }
        if ("TRANSACTIONS_ROLLED_BACK".equals(key)) {
            return true;
        }
        if ("TRANSFORM".equals(key)) {
            return true;
        }
        if ("TRANSFORMS".equals(key)) {
            return true;
        }
        if ("TRANSLATE".equals(key)) {
            return true;
        }
        if ("TRANSLATION".equals(key)) {
            return true;
        }
        if ("TREAT".equals(key)) {
            return true;
        }
        if ("TRIGGER".equals(key)) {
            return true;
        }
        if ("TRIGGER_CATALOG".equals(key)) {
            return true;
        }
        if ("TRIGGER_NAME".equals(key)) {
            return true;
        }
        if ("TRIGGER_SCHEMA".equals(key)) {
            return true;
        }
        if ("TRIM".equals(key)) {
            return true;
        }
        if ("TRUE".equals(key)) {
            return true;
        }
        if ("TRUNCATE".equals(key)) {
            return true;
        }
        if ("TRUSTED".equals(key)) {
            return true;
        }
        if ("TSEQUAL".equals(key)) {
            return true;
        }
        if ("TYPE".equals(key)) {
            return true;
        }
        if ("UESCAPE".equals(key)) {
            return true;
        }
        if ("UID".equals(key)) {
            return true;
        }
        if ("UNBOUNDED".equals(key)) {
            return true;
        }
        if ("UNCOMMITTED".equals(key)) {
            return true;
        }
        if ("UNDER".equals(key)) {
            return true;
        }
        if ("UNDO".equals(key)) {
            return true;
        }
        if ("UNENCRYPTED".equals(key)) {
            return true;
        }
        if ("UNION".equals(key)) {
            return true;
        }
        if ("UNIQUE".equals(key)) {
            return true;
        }
        if ("UNKNOWN".equals(key)) {
            return true;
        }
        if ("UNLISTEN".equals(key)) {
            return true;
        }
        if ("UNLOCK".equals(key)) {
            return true;
        }
        if ("UNNAMED".equals(key)) {
            return true;
        }
        if ("UNNEST".equals(key)) {
            return true;
        }
        if ("UNSIGNED".equals(key)) {
            return true;
        }
        if ("UNTIL".equals(key)) {
            return true;
        }
        if ("UPDATE".equals(key)) {
            return true;
        }
        if ("UPDATETEXT".equals(key)) {
            return true;
        }
        if ("UPPER".equals(key)) {
            return true;
        }
        if ("USAGE".equals(key)) {
            return true;
        }
        if ("USE".equals(key)) {
            return true;
        }
        if ("USER".equals(key)) {
            return true;
        }
        if ("USER_DEFINED_TYPE_CATALOG".equals(key)) {
            return true;
        }
        if ("USER_DEFINED_TYPE_CODE".equals(key)) {
            return true;
        }
        if ("USER_DEFINED_TYPE_NAME".equals(key)) {
            return true;
        }
        if ("USER_DEFINED_TYPE_SCHEMA".equals(key)) {
            return true;
        }
        if ("USING".equals(key)) {
            return true;
        }
        if ("UTC_DATE".equals(key)) {
            return true;
        }
        if ("UTC_TIME".equals(key)) {
            return true;
        }
        if ("UTC_TIMESTAMP".equals(key)) {
            return true;
        }
        if ("VACUUM".equals(key)) {
            return true;
        }
        if ("VALID".equals(key)) {
            return true;
        }
        if ("VALIDATE".equals(key)) {
            return true;
        }
        if ("VALIDATOR".equals(key)) {
            return true;
        }
        if ("VALUE".equals(key)) {
            return true;
        }
        if ("VALUES".equals(key)) {
            return true;
        }
        if ("VAR_POP".equals(key)) {
            return true;
        }
        if ("VAR_SAMP".equals(key)) {
            return true;
        }
        if ("VARBINARY".equals(key)) {
            return true;
        }
        if ("VARCHAR".equals(key)) {
            return true;
        }
        if ("VARCHAR2".equals(key)) {
            return true;
        }
        if ("VARCHARACTER".equals(key)) {
            return true;
        }
        if ("VARIABLE".equals(key)) {
            return true;
        }
        if ("VARIABLES".equals(key)) {
            return true;
        }
        if ("VARYING".equals(key)) {
            return true;
        }
        if ("VERBOSE".equals(key)) {
            return true;
        }
        if ("VIEW".equals(key)) {
            return true;
        }
        if ("VOLATILE".equals(key)) {
            return true;
        }
        if ("WAITFOR".equals(key)) {
            return true;
        }
        if ("WHEN".equals(key)) {
            return true;
        }
        if ("WHENEVER".equals(key)) {
            return true;
        }
        if ("WHERE".equals(key)) {
            return true;
        }
        if ("WHILE".equals(key)) {
            return true;
        }
        if ("WIDTH_BUCKET".equals(key)) {
            return true;
        }
        if ("WINDOW".equals(key)) {
            return true;
        }
        if ("WITH".equals(key)) {
            return true;
        }
        if ("WITHIN".equals(key)) {
            return true;
        }
        if ("WITHOUT".equals(key)) {
            return true;
        }
        if ("WORK".equals(key)) {
            return true;
        }
        if ("WRITE".equals(key)) {
            return true;
        }
        if ("WRITETEXT".equals(key)) {
            return true;
        }
        if ("X509".equals(key)) {
            return true;
        }
        if ("XOR".equals(key)) {
            return true;
        }
        if ("YEAR".equals(key)) {
            return true;
        }
        if ("YEAR_MONTH".equals(key)) {
            return true;
        }
        if ("ZEROFILL".equals(key)) {
            return true;
        }
        if ("ZONE".equals(key)) {
            return true;
        }

        return false;
    }


}
