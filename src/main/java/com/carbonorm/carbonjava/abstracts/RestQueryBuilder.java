package com.carbonorm.carbonjava.abstracts;

import com.carbonorm.carbonjava.throwables.PublicAlert;
import com.carbonorm.carbonjava.throwables.ThrowableHandler;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class RestQueryBuilder extends RestQueryValidation {

    private static Map<String, Object> injection = new HashMap<>();

    private Connection connection;

    public DatabaseUtility(String url, String user, String password) throws SQLException {
        this.connection = DriverManager.getConnection(url, user, password);
    }

    public int executeUpdate(String sql, ParameterMap parameters) throws SQLException {
        String parsedSql = sql;
        for (String key : parameters.keySet()) {
            parsedSql = parsedSql.replace(key, "?");
        }

        try (PreparedStatement statement = connection.prepareStatement(parsedSql)) {
            int index = 1;
            for (Object value : parameters.values()) {
                statement.setObject(index++, value);
            }

            return statement.executeUpdate();
        }
    }

    public static String parseAggregateWithNoOperators(String aggregate) {
        if (aggregate.equals(COUNT_ALL)) {
            aggregate = "COUNT(*)";
        } else {
            aggregate += "()";
        }
        return aggregate;
    }

    public String handleSubSelectAggregate(ArrayList<Object> stmt) throws Exception {

        String sql = "";

        String tableName = stmt.get(1).toString();

        String[] splitTableName = tableName.split("_");

        for (int i = 0; i < splitTableName.length; i++) {

            splitTableName[i] = Character.toUpperCase(splitTableName[i].charAt(0)) + splitTableName[i].substring(1);

        }

        tableName = String.join("_", splitTableName);

        if (!allowSubSelectQueries) {

            throw new Exception("Whoa, looks like a sub-select was encountered without " + tableName + "::$allowSubSelectQueries explicitly set to true. "
                    + " This is a security precaution and is required to only be set to true when explicitly needed. You should"
                    + " consider doing this in the " + tableName + "::PHP_VALIDATION['PREPROCESS'] event cycle.");

        }

        try {

            Class<?> self = getClass();

            int setLength = stmt.size();

            String as = "";

            Object primaryNames = getPrimaryFieldValue();

            ArrayList<Object> argv = new ArrayList<Object>();

            // Check the type of primaryField and act accordingly
            if (primaryNames == null) {

                switch (setLength) {

                    case 4:
                        as = stmt.get(3).toString();
                        // Intentional fall-through to case 3

                    case 3:

                        if (!(stmt.get(2) instanceof ArrayList<?>)) {

                            throw new IllegalArgumentException("The third argument passed to the restful sub-select was not an array. The signature for tables without primary keys is as follows:: [ Rest::SELECT, '" + tableName + "' , (array) $argv, (string|optional) $as]. The infringing set on table (" + getClass().getName() + ") :: (" + stmt.toString() + ")'");

                        }

                        //noinspection unchecked
                        argv = (ArrayList<Object>) stmt.get(2);

                        if (!(stmt.get(3) instanceof String)) {

                            throw new IllegalArgumentException("The fourth argument passed to the restful sub-select was not a string. The signature for tables without primary keys is as follows:: [ Rest::SELECT, '" + tableName + "' , (array) $argv, (string|optional) $as]. The infringing set on table (" + getClass().getName() + ") :: (" + stmt.toString() + ")'");

                        }

                        break;

                    default:

                        throw new IllegalArgumentException("The restful sub-select set passed was not the correct length. The table (" + tableName + ") has no primary keys so only three or four values are expected. Arguments must match the signature [ SELECT, " + tableName + ", $argv = [], $as = '' ] for sub-select queries on tables which have no SQL queries. The infringing set on table (" + yourClass.class + ") :: (" + stmt.toString() + ")");

                }

            } else if (primaryNames instanceof ArrayList<?> || primaryNames instanceof String) {

                String finalTableName = tableName;
                Object finalPrimary = primaryNames;
                Function<String, String> errorContext = new Function<String, String>() {
                    @Override
                    public String apply(String s) {

                        String className = getClass().getName();

                        String set = " The infringing set on table (" + className + ") :: (" + stmt.toString() + ")";

                        return (finalPrimary instanceof ArrayList<?>)
                                ? "The signature for tables with multiple column primary keys is as follows:: [ Rest::SELECT, '" + finalTableName + "',  (array) $primary, (array) $argv, (string|optional) $as]. " + set
                                : "The signature for tables with a single column primary key is as follows:: [ Rest::SELECT, '" + finalTableName + "',  (string|int|array) $primary, (array) $argv, (string|optional) $as]. Note: if an array is given it must be a key value with the key being the fully qualified `table.column` name associated with the primary key. " + set;

                    }
                };

                switch (setLength) {
                    case 5:
                        as = (String) stmt.get(4);
                        // Intentional fall-through to case 4
                    case 4:

                        if (stmt.get(2) instanceof String) {
                            primaryNames = stmt.get(2);
                        } else if (stmt.get(2) instanceof ArrayList<?>) {
                            primaryNames = stmt.get(2);
                        } else {
                            throw new IllegalArgumentException("The third argument passed to the restful sub-select was not a { null, string, int, or array }. " + errorContext.apply(tableName);
                        }

                        //noinspection unchecked - todo - check this
                        argv = (ArrayList<Object>) stmt.get(3);

                        if ("".equals(primaryNames)) {
                            primaryNames = null;
                        }

                        if (primaryNames instanceof Integer || primaryNames instanceof String) {

                            primaryNames = Collections.singletonMap(primaryNames, primaryNames);
                        }

                        if (primaryNames != null && !(primaryNames instanceof Map<?, ?>)) {
                            throw new IllegalArgumentException("The third argument passed to the restful sub-select was not a { null, string, int, or array }. " + errorContext.apply(tableName));
                        }

                        if (argv == null) {
                            throw new IllegalArgumentException("The fourth argument passed to the restful sub-select was not an array. " + errorContext.apply(tableName));
                        }

                        if (as == null) {
                            throw new IllegalArgumentException("The fourth argument passed to the restful sub-select was not a string. " + errorContext.apply(tableName));
                        }
                        break;

                    default:
                        throw new IllegalArgumentException("The restful sub-select set passed was not the correct length. (" + stmt.size() + ") " + errorContext.apply(tableName));
                }
            } else {
                throw new PublicAlert("Failed to process the restful sub-select set passed. The primary key field was not null, string, or array.");
            }


            // Assuming GET, REST_REQUEST_PARAMETERS, and PDO_TYPE are constants within the context
            self.getMethod("startRest", String.class, Object.class, Object.class, ArrayList.class, boolean.class)
                    .invoke(null, GET, REST_REQUEST_PARAMETERS, primaryNames, argv, true);

            sql = (String) self.getMethod("buildSelectQuery", Object.class, ArrayList.class, boolean.class)
                    .invoke(null, primaryNames, argv, true);

            sql = "(" + sql + ")";

            if (as != null && !as.isEmpty()) {

                // Assuming addInjection is a method defined within the context
                String injection = (String) self.getMethod("addInjection", String.class, Map.class)
                        .invoke(null, as, Collections.singletonMap(PDO_TYPE, null));

                sql = sql + " AS " + injection;

            }

            self.getMethod("completeRest", boolean.class).invoke(null, true);

        } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
            ThrowableHandler.generateLog(e);
        }

        return sql;


    }





}
