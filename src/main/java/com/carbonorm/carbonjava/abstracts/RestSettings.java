package com.carbonorm.carbonjava.abstracts;

import com.carbonorm.carbonjava.Database;
import com.carbonorm.carbonjava.interfaces.IRest;

import java.util.*;

abstract class RestSettings extends Database implements IRest {

    public List<Object[]> activeQueryStates = new ArrayList<>();

    public String REST_REQUEST_METHOD = null;

    // Java does not support mixed type. Use Object as a generic type
    public Object REST_REQUEST_PRIMARY_KEY = null;

    // Constants in Java can't be complex data types (like arrays or lists).
    // Instead, an unmodifiable list is used
    public final String REST_REQUEST_PRIMARY_KEY_CONST = "REST_REQUEST_PRIMARY_KEY";

    // Java does not support mixed type. Use Object as a generic type
    public Object REST_REQUEST_PARAMETERS = null;

    public Object REST_REQUEST_RETURN_DATA = null; // this could be a single array or array of arrays

    public List<String> VALIDATED_REST_COLUMNS = new ArrayList<>();

    public List<String> compiled_valid_columns = new ArrayList<>();

    public List<String> compiled_PDO_validations = new ArrayList<>();

    public List<String> compiled_PHP_validations = new ArrayList<>();

    public List<String> compiled_regex_validations = new ArrayList<>();

    public List<String> join_tables = new ArrayList<>();

    public List<String> injection = new ArrayList<>();

    public boolean commit = true;
    public boolean allowInternalMysqlFunctions = true;
    public boolean allowSubSelectQueries = false;
    public boolean externalRestfulRequestsAPI = false;
    public boolean jsonReport = true;

    public boolean aggregateSelectEncountered = false;
    public boolean columnSelectEncountered = false;

    public boolean allowFullTableUpdates = false;
    public boolean allowFullTableDeletes = false;

    public boolean suppressErrorsAndReturnFalse = false;


    // Add all other constants in the same manner ...

    // Declare the map as a static final member variable
    public static final Map<String, String> SQL_VERSION_PREG_REPLACE;
    static {
        Map<String, String> map = new HashMap<>();

        map.put("bigint\\(\\d+\\)", "bigint");
        map.put("int\\(\\d+\\)", "int");
        map.put("CHARACTER\\sSET\\s[A-Za-z0-9_]+", "");
        map.put("COLLATE\\s[A-Za-z0-9_]+", "");
        map.put("datetime\\sDEFAULT\\sNULL", "datetime");
        map.put("\\sON\\sDELETE\\sNO\\sACTION", "");
        map.put("AUTO_INCREMENT=\\d+", "");
        map.put("COLLATE=[A-Za-z0-9_]+", "");
        map.put("CREATE\\sTABLE\\s`", "CREATE TABLE IF NOT EXISTS `");
        map.put("DEFAULT CHARSET=[A-Za-z0-9_]+", "");
        map.put("ON DELETE NO ACTION", " ");
        map.put("ON UPDATE NO ACTION", " ");
        map.put("\\s{2,}", " ");
        map.put("([,;])$", "");
        map.put("(\\s*)$", "");

        SQL_VERSION_PREG_REPLACE = Collections.unmodifiableMap(map);
    }
}
