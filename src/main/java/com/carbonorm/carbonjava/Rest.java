package com.carbonorm.carbonjava;

import com.carbonorm.carbonjava.abstracts.RestLifeCycle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;
import java.util.function.Function;

public class Rest extends RestLifeCycle {

    public Rest(Map<String, Object> returnMap) {
        super(returnMap);
    }

    protected static Object insert(Map<String, Object> postRequestBody) {
        String ignore = "";

        // Check if postRequestBody is an array and has an IGNORE key
        if (postRequestBody.size() == 1
                && postRequestBody.containsKey("IGNORE")
                && postRequestBody.get("IGNORE") instanceof List) {

            postRequestBody = (Map<String, Object>) postRequestBody.get("IGNORE");

            ignore = " " + "IGNORE" + " ";

        }

        int tries = 0;
        do {
            try {
                startRest("POST", new ArrayList[]{new ArrayList<>()}, postRequestBody);

                // format the data if multiple rows are to be posted at the same time
                if (!postRequestBody.isEmpty() && hasStringKeys(postRequestBody.get(0))) {
                    postRequestBody = Collections.singletonList(postRequestBody);
                }

                // loop through each row of new values
                for (Map<String, Object> iValue : postRequestBody) {
                    // loop through and validate each of the values (column names)
                    for (Map.Entry<String, Object> entry : iValue.entrySet()) {
                        String columnName = entry.getKey();
                        Object postValue = entry.getValue();

                        if (!COLUMNS.containsKey(columnName)) {
                            return signalError("Restful table (" + YourClass.class + ") would not post column (" + columnName + "), because the column does not appear to exist.");
                        }

                        if (AUTO_ESCAPE_POST_HTML_SPECIAL_CHARS) {
                            if (postValue instanceof String) {
                                postValue = escapeHtmlSpecialChars((String) postValue);
                            }
                        }

                        entry.setValue(postValue);
                    }
                }

                StringBuilder keys = new StringBuilder();
                List<String> pdoValues = new ArrayList<>();
                List<String> boundValues = new ArrayList<>();

                int rowsToInsert = postRequestBody.size();
                int totalKeys = 0;
                int i = 0;
                int firstKey = postRequestBody.isEmpty() ? 0 : postRequestBody.get(0).keySet().stream().findFirst().orElse(null);
                List<String> firstRowKeys = new ArrayList<>(postRequestBody.get(firstKey).keySet());

                do {
                    pdoValues.add("");
                    String op = "EQUAL";
                    for (Map.Entry<String, String> entry : COLUMNS.entrySet()) {
                        String fullName = entry.getKey();
                        String shortName = entry.getValue();
                        boolean canSkip = PDO_VALIDATION.containsKey(fullName) && (boolean) PDO_VALIDATION.get(fullName).getOrDefault("SKIP_COLUMN_IN_POST", false);
                        if (canSkip && !firstRowKeys.contains(fullName)) {
                            continue;
                        }

                        if (i == 0) {
                            totalKeys++;
                            keys.append(shortName).append(", ");
                        }

                        shortName += i;
                        boundValues.add(shortName);

                        pdoValues.set(i, pdoValues.get(i) + ("binary".equals(PDO_VALIDATION.get(fullName).get("MYSQL_TYPE")) ? "UNHEX(:" + shortName + "), " : ":" + shortName + ", "));

                        validateInternalColumn(fullName, op, postRequestBody.get(i).get(fullName));
                    }

                    pdoValues.set(i, pdoValues.get(i).replaceAll(", $", ""));
                    i++;
                } while (i < rowsToInsert);

                if (totalKeys == 0) {
                    return signalError("An unexpected error has occurred, please open a support ticket at https://github.com/RichardTMiles/CarbonPHP/issues.");
                }

                String sql = "INSERT" + ignore + " INTO "
                        + (QUERY_WITH_DATABASE ? DATABASE + "." : "")
                        + TABLE_NAME + " ("
                        + keys.substring(0, keys.length() - 2)
                        + ") VALUES ("
                        + String.join("), (", pdoValues) + ")";

                boolean primaryBinary = PRIMARY instanceof String && "binary".equals(PDO_VALIDATION.get(PRIMARY).get("MYSQL_TYPE"));

                if (primaryBinary) {
                    Connection pdo = database(false);
                    if (!pdo.isInTransaction()) {
                        pdo.beginRequest();
                    }
                }

                Function<ResultSet, Void> moreReporting = jsonSQLReporting(getArguments(), sql);

                postpreprocessRestRequest(sql);

                Connection pdo = database(false);

                PreparedStatement stmt = pdo.prepareStatement(sql);

                String op = "EQUAL";
                i = 0;

                do {
                    Map<String, Object> iValue = postRequestBody.size() > i ? postRequestBody.get(i) : new HashMap<>();
                    for (Map.Entry<String, Map<String, Object>> entry : PDO_VALIDATION.entrySet()) {
                        String fullName = entry.getKey();
                        Map<String, Object> info = entry.getValue();

                        boolean canSkip = (boolean) info.getOrDefault("SKIP_COLUMN_IN_POST", false);

                        if (canSkip) {
                            boolean isExplicitlySet = iValue.containsKey(fullName);

                            if (!isExplicitlySet) {
                                continue;
                            }

                            if (CURRENT_TIMESTAMP.equals(info.getOrDefault("DEFAULT_POST_VALUE", ""))) {
                                return signalError("The column (" + fullName + ") is set to default to CURRENT_TIMESTAMP. The Rest API does not allow POST requests with columns explicitly set whose default is CURRENT_TIMESTAMP. You can remove the default in MySQL or the column (" + fullName + ") from the request.");
                            }
                        }

                        String shortName = COLUMNS.get(fullName) + i;

                        int key = boundValues.indexOf(shortName);

                        if (key == -1) {
                            return signalError("An internal rest error has occurred where rest attempted binding (" + shortName + ") which was not found in the prepared sql (" + sql + ")");
                        }

                        boundValues.remove(key);

                        if (PRIMARY.equals(fullName) || (PRIMARY instanceof List && PRIMARY.contains(fullName))) {
                            iValue.putIfAbsent(fullName, false);
                            Object value = iValue.get(fullName);

                            if (value == null || value.equals(false)) {
                                value = CARBON_CARBONS_PRIMARY_KEY ? beginTransaction(YourClass.class, iValue.getOrDefault(DEPENDANT_ON_ENTITY, null)) : fetchColumn("SELECT (REPLACE(UUID() COLLATE utf8_unicode_ci,\"-\",\"\"))")[0];
                                iValue.put(fullName, value);
                            } else if (!validateInternalColumn(fullName, op, value)) {
                                throw new PublicAlert("The column value of (" + fullName + ") caused custom restful api validations for (" + YourClass.class + ") primary key to fail (" + JSON_PRETTY_PRINT.writeValueAsString(self::$compiled_valid_columns) + ").");
                            }

                            int maxLength = (int) info.getOrDefault("MAX_LENGTH", 0);
                            ((PreparedStatement) stmt).setObject(key + 1, value, (int) info.get("PDO_TYPE"), maxLength);
                        } else if ("json".equals(info.get("MYSQL_TYPE"))) {
                            if (!iValue.containsKey(fullName)) {
                                return signalError("Table (" + YourClass.class + ") column (" + fullName + ") is set to not null and has no default value. It must exist in the request and was not found in the one sent.");
                            }

                            if (!validateInternalColumn(fullName, op, iValue.get(fullName))) {
                                throw new PublicAlert("Your tables (" + YourClass.class + "), or joining tables, custom restful api validations caused the request to fail on json column (" + fullName + "). Possible values include (" + JSON_PRETTY_PRINT.writeValueAsString(self::$compiled_valid_columns) + ").");
                            }

                            if (!(iValue.get(fullName) instanceof String)) {
                                String json = JSON_PRETTY_PRINT.writeValueAsString(iValue.get(fullName));
                                iValue.put(fullName, json);
                            }

                            op = "EQUAL";
                            runCustomCallables(fullName, op, iValue.get(fullName));

                            ((PreparedStatement) stmt).setObject(key + 1, iValue.get(fullName), (int) info.get("PDO_TYPE"));
                        } else if (info.containsKey("DEFAULT_POST_VALUE")) {
                            iValue.putIfAbsent(fullName, info.get("DEFAULT_POST_VALUE"));

                            if (!validateInternalColumn(fullName, op, iValue.get(fullName), iValue.get(fullName).equals(info.get("DEFAULT_POST_VALUE")))) {
                                return signalError("Your custom restful table (" + YourClass.class + ") api validations caused the request to fail on column (" + fullName + ")");
                            }

                            op = "EQUAL";
                            runCustomCallables(fullName, op, iValue.get(fullName));

                            ((PreparedStatement) stmt).setObject(key + 1, iValue.get(fullName), (int) info.get("PDO_TYPE"));
                        } else {
                            if (!iValue.containsKey(fullName)) {
                                return signalError("Required argument (" + fullName + ") is missing from the request to (" + YourClass.class + ") and has no default value.");
                            }

                            if (!validateInternalColumn(fullName, op, iValue.get(fullName), info.containsKey("DEFAULT_POST_VALUE") ? iValue.get(fullName).equals(info.get("DEFAULT_POST_VALUE")) : false)) {
                                return signalError("Your custom restful api validations for (" + YourClass.class + ") caused the request to fail on required column (" + fullName + ").");
                            }

                            op = "EQUAL";
                            runCustomCallables(fullName, op, iValue.get(fullName));

                            ((PreparedStatement) stmt).setObject(key + 1, iValue.get(fullName), (int) info.get("PDO_TYPE"));
                        }
                    }
                    i++;
                } while (i < rowsToInsert);

                if (!boundValues.isEmpty()) {
                    return signalError("The insert query (" + sql + ") did not receive values for (" + String.join(", ", boundValues) + "). This is not expected, please open a ticket so we can fix this at (" + Documentation.GIT_SUPPORT + ").");
                }

                if (!((PreparedStatement) stmt).execute()) {
                    completeRest();
                    return signalError("The REST generated PreparedStatement failed to execute for (" + YourClass.class + "), with error :: " + JSON_THROW_ON_ERROR.writeValueAsString(stmt.getErrorInfo()));
                }

                if (moreReporting != null) {
                    moreReporting.apply(((PreparedStatement) stmt).getResultSet());
                }

                // Rest of the code related to AUTO_INCREMENT_PRIMARY_KEY
                // ...

                // Rest of the code related to committing and handling the result
                // ...

                return true;

            } catch (Throwable e) {
                handleRestException(e);
            }
            tries++;
        } while (tries != 3);

        return false;
    }

    protected boolean updateReplace(Map<String, Object> returnUpdated, Map<String, Object> argv, Map<String, Object> primary) {
        // Assume that the "startRest", "PUT", "DELETE", "WHERE", "REPLACE", "UPDATE", "COLUMNS", "EQUAL", "MYSQL_TYPE",
        // "PDO_VALIDATION", "MAX_LENGTH", "PDO_TYPE", "QUERY_WITH_DATABASE", "DATABASE", "TABLE_NAME", "CARBON_CARBONS_PRIMARY_KEY",
        // "PublicAlert", "Carbons", "jsonSQLReporting", "postpreprocessRestRequest", "buildBooleanJoinedConditions",
        // "bind", "signalError", "handleRestException", "verifyRowsAffected", "prepostprocessRestRequest", "commit", "json_encode"
        // functions are defined elsewhere.

        try {
            startRest("PUT", returnUpdated, argv, primary);

            boolean replace = false;
            Map<String, Object> where = new HashMap<>();

            if (argv.containsKey("WHERE")) {
                where = (Map<String, Object>) argv.get("WHERE");
                argv.remove("WHERE");
            }

            if (argv.containsKey("REPLACE")) {
                replace = true;
                argv = (Map<String, Object>) argv.get("REPLACE");
            } else if (argv.containsKey("UPDATE")) {
                argv = (Map<String, Object>) argv.get("UPDATE");
            }

            if (null == PRIMARY) {
                if (!allowFullTableUpdates && where.isEmpty()) {
                    return signalError("Restful tables which have no primary key must be updated using conditions given to `argv.get(\"WHERE\")` and values to be updated given to `argv.get(\"UPDATE\")`. No WHERE attribute given. To bypass this, set `allowFullTableUpdates = true;` during the PREPROCESS events, or just directly before this request.");
                }

                // todo - more validations on payload not empty
                if (argv.isEmpty()) {
                    return signalError("Restful tables which have no primary key must be updated using conditions given to `argv.get(\"WHERE\")` and values to be updated given to `argv.get(\"UPDATE\")`. No UPDATE attribute given.");
                }
            } else {
                List<String> primaryKeysNeeded = PRIMARY instanceof List ? (List<String>) PRIMARY : Collections.singletonList((String) PRIMARY);

                // lets check the root level of our where clause to see if we have all primary keys
                primary = primary != null ? primary : new HashMap<>();
                where.putAll(primary);

                boolean emptyWhere = where.isEmpty();

                for (String primaryKey : primaryKeysNeeded) {
                    if (!emptyWhere && !where.containsKey(primaryKey)) {
                        if (allowFullTableUpdates) {
                            continue;
                        }
                        return signalError("Restful tables which have a primary key must be updated by its primary key. To bypass this, you may set `allowFullTableUpdates = true;` during the PREPROCESS events. The primary key (" + primaryKey + ") was not found." + (primaryKeysNeeded.size() > 1 ? " Please make sure all pks (" + primaryKeysNeeded + ") are in the request." : ""));
                    }

                    if (replace) {
                        continue;
                    }

                    if (emptyWhere) {
                        where.put(primaryKey, primary.get(primaryKey));
                    }

                    // either way remove it from the update payload if it is unneeded
                    if (Objects.equals(where.get(primaryKey), argv.get(primaryKey))) {
                        argv.remove(primaryKey);
                    }
                }

                if (!replace && where.isEmpty()) {
                    throw new RuntimeException("The where is empty. Arguments were :: " + json_encode(Arrays.asList(where, primary)));
                }
            }

            for (String key : argv.keySet()) {
                if (!compiled_PDO_validations.containsKey(key)) {
                    return signalError("Restful table could not update column " + key + ", because it does not appear to exist. Please re-run RestBuilder if you believe this is incorrect.");
                }

                String op = EQUAL;
                if (!validateInternalColumn(key, op, argv.get(key))) {
                    return signalError("Your custom restful API validations caused the request to fail on column (" + key + ").");
                }
            }

            String updateOrReplace = replace ? REPLACE : UPDATE;

            String sql = updateOrReplace + " " + (QUERY_WITH_DATABASE ? DATABASE + "." : "") + TABLE_NAME + " SET ";

            StringBuilder set = new StringBuilder();

            for (String fullName : argv.keySet()) {

                String shortName = COLUMNS.get(fullName);

                set.append(" ").append(fullName).append(" = ");

                if (MYSQL_TYPE_BINARY.equals(compiled_PDO_validations.get(fullName).get(MYSQL_TYPE))) {

                    set.append("UNHEX(:").append(shortName).append(") ,");

                } else {

                    set.append(":").append(shortName).append(" ,");

                }

            }

            sql += set.substring(0, set.length() - 1);

            Connection pdo = database(false);

            if (!pdo.isInTransaction() && !pdo.beginRequest()) {
                throw new RuntimeException("Failed to start a PDO transaction for the restful Put request!");
            }

            if (replace) {
                if (!where.isEmpty()) {
                    return signalError("Replace queries may not be given a where clause. Use Put instead.");
                }
            } else if (!allowFullTableUpdates || !where.isEmpty()) {
                if (where.isEmpty()) {
                    throw new RuntimeException("The where clause is required but has been detected as empty. Arguments were :: " + json_encode(Arrays.asList(where, primary)));
                }

                sql += " WHERE " + buildBooleanJoinedConditions(where);
            }

            Function<ResultSet, Void> moreReporting = jsonSQLReporting(getArguments(), sql);

            postpreprocessRestRequest(sql);

            PreparedStatement stmt = pdo.prepareStatement(sql);
            if (stmt == null) {
                return signalError("PDO failed to prepare the sql generated! (" + sql + ")");
            }

            for (String fullName : COLUMNS.keySet()) {
                if (argv.containsKey(fullName)) {
                    String op = EQUAL;
                    if (!validateInternalColumn(fullName, op, argv.get(fullName))) {
                        return signalError("Your custom restful API validations caused the request to fail on column (" + fullName + ").");
                    }

                    if ("".equals(PDO_VALIDATION.get(fullName).get(MAX_LENGTH))) { // does length exist
                        Object value = PDO_VALIDATION.get(fullName).get(MYSQL_TYPE).equals("json")
                                ? json_encode(argv.get(fullName))
                                : argv.get(fullName);

                        if (!stmt.bindValue(":" + COLUMNS.get(fullName), value, PDO_VALIDATION.get(fullName).get(PDO_TYPE))) {
                            return signalError("Failed to bind (:" + COLUMNS.get(fullName) + ") with value (" + value + ")");
                        }
                    } else if (!stmt.bindParam(":" + COLUMNS.get(fullName), argv.get(fullName),
                            PDO_VALIDATION.get(fullName).get(PDO_TYPE),
                            (int) PDO_VALIDATION.get(fullName).get(MAX_LENGTH))) {
                        return signalError("Failed to bind (:" + COLUMNS.get(fullName) + ") with value (" + argv.get(fullName) + ")");
                    }
                }
            }

            bind(stmt);

            if (!stmt.execute()) {
                completeRest();
                return signalError("The REST generated PreparedStatement failed to execute with error :: " + json_encode(stmt.getError(), JSON_THROW_ON_ERROR | JSON_PRETTY_PRINT));
            }

            if (moreReporting != null) {
                moreReporting.apply(stmt.getResultSet());
            }

            if (!verifyRowsAffected(stmt, updateOrReplace, sql, primary, argv)) {
                return false;
            }

            Map<String, Object> updatedValues = new HashMap<>();
            for (String fullName : COLUMNS.keySet()) {
                String shortName = COLUMNS.get(fullName);
                if (argv.containsKey(fullName)) {
                    updatedValues.put(shortName, argv.get(fullName));
                }
            }
            returnUpdated.putAll(updatedValues);

            prepostprocessRestRequest(returnUpdated);

            if (commit) {
                if (!Database.commit()) {
                    return signalError("Failed to store commit transaction on table {{TableName}}");
                }

                if (moreReporting != null) {
                    moreReporting.apply(null);
                }
            }

            postprocessRestRequest(returnUpdated);
            completeRest();
            return true;

        } catch (Throwable e) {
            handleRestException(e);
        }
        return false;
    }

    private static String json_encode(Object value) {
        // Implement your own json_encode function here
        return "";
    }


    protected static boolean remove(Object[] remove, Object[] argv, Object[] primary) {
        // Assume that the "startRest", "DELETE", "database", "WHERE", "JOIN", "CARBON_CARBONS_PRIMARY_KEY",
        // "PublicAlert", "Carbons", "jsonSQLReporting", "postpreprocessRestRequest", "buildBooleanJoinedConditions",
        // "buildSelectQuery", "signalError", "handleRestException", "verifyRowsAffected", "prepostprocessRestRequest",
        // "commit", "json_decode" functions are defined elsewhere.

        int tries = 0; // Initialize the tries counter

        do {
            try {
                startRest("DELETE", remove, argv, primary);

                if (argv.length >= 1 && !arrayContainsKey(argv, "WHERE") && !arrayContainsKey(argv, "JOIN")) {
                    argv = new Object[]{map("WHERE", argv)};
                }

                if (!arrayContainsKey(argv, "WHERE")) {
                    argv = arrayMerge(argv, new Object[]{"WHERE", new Object[]{}});
                }

                Connection connection = database(false);

                boolean emptyPrimary = primary == null || primary.length == 0;

                if (CARBON_CARBONS_PRIMARY_KEY && !emptyPrimary) {
                    if (primary instanceof String) {
                        primary = new Object[]{primary};
                    }

                    return Carbons.Delete(remove, primary, argv);
                }

                if (!allowFullTableDeletes && emptyPrimary && ((Object[]) argv[1]).length == 0) {
                    return signalError("When deleting from restful tables a primary key or where query must be provided.");
                }

                String queryDatabaseName = QUERY_WITH_DATABASE ? DATABASE + "." : "";
                String tableName = queryDatabaseName + TABLE_NAME;

                String sql;
                if (CARBON_CARBONS_PRIMARY_KEY) {
                    if (primary instanceof String) {
                        primary = new Object[]{primary};
                    }

                    throw new RuntimeException("Tables which use carbon for indexes should not have composite primary keys.");
                } else {
                    sql = "DELETE FROM " + tableName + " ";

                    boolean emptyWhere = ((Object[]) argv[1]).length == 0;
                    if (!allowFullTableDeletes && emptyPrimary && emptyWhere) {
                        return signalError("When deleting from restful tables a primary key or where query must be provided."
                                + " This can be disabled by setting `allowFullTableDeletes = true;` during the PREPROCESS events,"
                                + " or just directly before this request.");
                    }

                    Object[] argvWhere = (Object[]) argv[1];

                    if (PRIMARY instanceof String && !emptyPrimary) {
                        argvWhere = arrayMerge(argvWhere, primary);
                    }

                    String where = buildBooleanJoinedConditions(argvWhere);
                    boolean emptyWhereAfterMerge = where.isEmpty();

                    if (emptyWhereAfterMerge && !allowFullTableDeletes) {
                        return signalError("The where condition provided appears invalid.");
                    }

                    if (!emptyWhereAfterMerge) {
                        sql += "WHERE " + where;
                    }
                }

                if (!connection.getAutoCommit()) {
                    connection.setAutoCommit(true);
                }

                Function<ResultSet, Void> moreReporting = jsonSQLReporting(getArguments(), sql);

                postpreprocessRestRequest(sql);

                PreparedStatement stmt = connection.prepareStatement(sql);

                bind(stmt);

                if (!stmt.execute()) {
                    completeRest();
                    return signalError("The REST generated PreparedStatement failed to execute with error :: "
                            + json_encode(stmt.getError(), JSON_THROW_ON_ERROR | JSON_PRETTY_PRINT));
                }

                if (moreReporting != null) {
                    moreReporting.apply(stmt.getResultSet());
                }

                if (!verifyRowsAffected(stmt, REST_REQUEST_METHOD, sql, primary, argv)) {
                    return false;
                }

                // todo - should this pass as ref? and be emptied later?
                Object[] removeWhere = (Object[]) argv[1];
                prepostprocessRestRequest(removeWhere);

                if (commit) {
                    if (!Database.commit()) {
                        return signalError("Failed to store commit transaction on table {{TableName}}");
                    }

                    if (moreReporting != null) {
                        moreReporting.apply(null);
                    }
                }

                postprocessRestRequest(removeWhere);
                remove = new Object[]{};

                completeRest();
                return true;

            } catch (Throwable e) {
                handleRestException(e);
            }

            tries++;
        } while (tries < 3);

        return false;
    }

    private static boolean arrayContainsKey(Object[] arr, String key) {
        for (Object obj : arr) {
            if (obj instanceof Map && ((Map) obj).containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    private static Object[] arrayMerge(Object[] arr1, Object[] arr2) {
        List<Object> mergedList = new ArrayList<>();
        for (Object obj : arr1) {
            mergedList.add(obj);
        }
        for (Object obj : arr2) {
            mergedList.add(obj);
        }
        return mergedList.toArray(new Object[0]);
    }

    private static Map<String, Object> map(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }


    protected static boolean select(Object[] returnData, Object[] argv, Object[] primary, Object... fetchOptions) {
        // Assume that the "startRest", "GET", "database", "LOCK", "getTransaction", "beginTransaction",
        // "PublicAlert", "jsonSQLReporting", "postpreprocessRestRequest", "buildSelectQuery",
        // "bind", "signalError", "handleRestException", "json_decode" functions are defined elsewhere.

        List<String> selectSQLs = new ArrayList<>();

        int tries = 0; // Initialize the tries counter

        do {
            try {
                startRest("GET", returnData, argv, primary);

                boolean isLock = false;
                for (Object arg : argv) {
                    if (arg.equals("LOCK")) {
                        isLock = true;
                        break;
                    }
                }

                // If we need to use table or row level locks, we should use the main writer instance.
                Connection connection = database(!isLock);

                if (isLock && !connection.getAutoCommit()) {
                    connection.setAutoCommit(false);
                }

                if (primary != null && !(primary instanceof Object[])) {
                    throw new RuntimeException("Looks like your restful validations changed the primary value to an invalid state."
                            + " The primary field should be null or an array with the following syntax :: [ Table.EXAMPLE_COLUMN => \"primary_key_string\" ]"
                            + " The value (" + json_encode(primary) + ") was instead received. ");
                }

                if (!(argv instanceof Object[])) {
                    throw new RuntimeException("Looks like your restful validations changed the argv value to an invalid state."
                            + " The argv was not an array. Received :: (" + json_encode(argv) + ")");
                }

                String sql = buildSelectQuery(primary, argv);
                Function<ResultSet, Void> moreReporting = jsonSQLReporting(getArguments(), sql);

                postpreprocessRestRequest(sql);

                selectSQLs.add(sql);

                PreparedStatement stmt = connection.prepareStatement(sql);

                bind(stmt);

                if (!stmt.execute()) {
                    completeRest();
                    return signalError("The REST generated PreparedStatement failed to execute with error :: "
                            + json_encode(stmt.getError(), JSON_THROW_ON_ERROR | JSON_PRETTY_PRINT));
                }

                if (moreReporting != null) {
                    moreReporting.apply(stmt.getResultSet());
                }

                int fetchOptionsSize = fetchOptions.length;
                int fetchOptionsType = (fetchOptionsSize > 0) ? (Integer) fetchOptions[0] : ResultSet.FETCH_FORWARD;

                boolean fetchingIntoObjects = (fetchOptionsSize > 0) && (fetchOptionsType == ResultSet.FETCH_CLASS);

                ResultSet rs = stmt.getResultSet();

                // Fetch data into an ArrayList of HashMaps.
                List<Map<String, Object>> fetchedData = new ArrayList<>();
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> rowData = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = rsmd.getColumnName(i);
                        Object value = rs.getObject(i);
                        rowData.put(columnName, value);
                    }
                    fetchedData.add(rowData);
                }

                rs.close();
                stmt.close();
                connection.close();

                // Continue with the rest of the code (processing fetchedData and JSON_COLUMNS)
                // ...

                postprocessRestRequest(returnData);
                completeRest();
                return true;
            } catch (Throwable e) {
                handleRestException(e);
            }

            tries++;
        } while (tries < 3);

        return false;
    }


    private static boolean verifyRowsAffected(PreparedStatement stmt, String method, String sql, String[] primary, Object[] argv) {
        boolean allPrimaryKeysGiven = primary != null && ((primary.length == 1 && PRIMARY instanceof String) || primary.length == PRIMARY.length);

        int rowCount = stmt.getUpdateCount();

        if (EXTERNAL_RESTFUL_REQUESTS_API) {
            if (rowCount >= 0) {
                // If the rowCount is non-negative, it indicates the number of rows affected by the update
                JSON_GLOBALS.put("rowCount", rowCount);
            }
        }

        if (rowCount == 0 && allPrimaryKeysGiven) {
            boolean pdoOptionFoundRows = Database.getPdoOptions().getOrDefault(ConnectionProvider.MYSQL_ATTR_FOUND_ROWS, false);

            if (!pdoOptionFoundRows) {
                return signalError("Zero rows were updated. MySQL failed to update any target(s) during (" + method + ") on "
                        + "table (" + TABLE_NAME + ") while executing query (" + sql + "). The arguments passed to rest are ("
                        + JSON_PRETTY_PRINT.writeValueAsString(argv) + ") and primary key(s) ("
                        + JSON_PRETTY_PRINT.writeValueAsString(primary) + "). By default, CarbonPHP passes "
                        + "PDO::MYSQL_ATTR_FOUND_ROWS => false, to the PDO driver; aka return the number of affected rows, "
                        + "not the number of rows found. No changes have been made to this configuration. Your issue may only be "
                        + "that the target does not need updates, or no rows exist based on query/table conditions."
                        + " ([SQLSTATE, Driver-specific error code, Driver-specific error message]) :: "
                        + JSON_THROW_ON_ERROR.writeValueAsString(stmt.getErrorInfo()) + ")");
            }

            return signalError("Zero rows were found for update! MySQL failed to find any target(s) to update during (" + method + ") on "
                    + "table (" + TABLE_NAME + ") while executing query (" + sql + "). The arguments passed to rest are ("
                    + JSON_PRETTY_PRINT.writeValueAsString(argv) + ") and primary key(s) ("
                    + JSON_PRETTY_PRINT.writeValueAsString(primary) + "). CarbonPHP has been overridden and passes "
                    + "PDO::MYSQL_ATTR_FOUND_ROWS => true, to the PDO driver; aka return the number of found rows, "
                    + "not the number of rows affected. ([SQLSTATE, Driver-specific error code, Driver-specific error message]) :: "
                    + JSON_THROW_ON_ERROR.writeValueAsString(stmt.getErrorInfo()) + ")");
        }

        return true;
    }

}
