package com.carbonorm.carbonjava.abstracts;

import com.carbonorm.carbonjava.CarbonJAVA;
import com.carbonorm.carbonjava.Database;
import com.carbonorm.carbonjava.throwables.PublicAlert;
import com.carbonorm.carbonjava.throwables.ThrowableHandler;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Map;

public abstract class RestLifeCycle extends RestQueryBuilder {

    protected void startRest(
            String method,
            Object[] returns,
            Object[] args,
            Object primary) {
        startRest(method, returns, args, primary, false);
    }

    @SuppressWarnings("SameParameterValue")
    protected void startRest(
            String method,
            Object[] returns,
            Object args,
            Object primary,
            boolean subQuery) {

        checkPrefix();

        if (REST_REQUEST_METHOD != null) {
            activeQueryStates.add(new Object[]{
                    REST_REQUEST_METHOD,
                    REST_REQUEST_PRIMARY_KEY,
                    REST_REQUEST_PARAMETERS,
                    REST_REQUEST_RETURN_DATA,
                    VALIDATED_REST_COLUMNS,
                    compiled_valid_columns,
                    compiled_PDO_validations,
                    compiled_PHP_validations,
                    compiled_regex_validations,
                    externalRestfulRequestsAPI,
                    join_tables,
                    aggregateSelectEncountered,
                    columnSelectEncountered,
                    injection
            });
            externalRestfulRequestsAPI = false;
            aggregateSelectEncountered = false;
            columnSelectEncountered = false;
        }

        REST_REQUEST_METHOD = method;
        REST_REQUEST_PRIMARY_KEY = primary;
        REST_REQUEST_PARAMETERS = args;
        REST_REQUEST_RETURN_DATA = returns;

        if (!subQuery) {
            VALIDATED_REST_COLUMNS = new Object[]{};
            compiled_valid_columns = new Object[]{};
            compiled_PDO_validations = new Object[]{};
            compiled_PHP_validations = new Object[]{};
            compiled_regex_validations = new Object[]{};
            join_tables = new Object[]{};
            injection = new Object[]{};
        }

        // We can't use late static binding in Java. Therefore, call the methods directly.
        gatherValidationsForRequest();
        preprocessRestRequest();
    }


    /**
     * This is the constructor for the RestLifeCycle class. Assign existing Map values to the class members
     */
    public RestLifeCycle(Map<String, Object> returnMap) {
        if (returnMap.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : returnMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            try {
                Field field = this.getClass().getDeclaredField(key);
                field.setAccessible(true);
                field.set(this, value);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }


    public boolean signalError(String message) throws PublicAlert {
        if (suppressErrorsAndReturnFalse == true
                && externalRestfulRequestsAPI == false
                && CarbonJAVA.is_running_production
                && CarbonJAVA.carbon_is_root == false
                && CarbonJAVA.test == false) {

            return false;
        }

        throw new PublicAlert(message);
    }

    /**
     * This will terminate 99% of the time, but the 1% it wasn't you need to rerun what you try caught
     * as the error was just the database going away.
     */
    public static void handleRestException(Throwable e) {
        if (e instanceof SQLException) {
            // This most likely terminates (only on db resource drop will it continue < 1%)
            Database.tryCatchSQLException((SQLException) e);
        } else {
            ThrowableHandler.generateLog(e); // This terminates
        }
    }


}
