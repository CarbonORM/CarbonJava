package com.carbonorm.carbonjava.interfaces;

public interface IRest {

    // mysql restful identifiers alphabetical
    String ADDDATE = "ADDDATE";
    String ADDTIME = "ADDTIME";
    String AS = "AS";
    String ASC = "ASC";

    String BETWEEN = "BETWEEN";

    String CONCAT = "CONCAT";
    String CONSTRAINT_NAME = "CONSTRAINT_NAME";
    String CONVERT_TZ = "CONVERT_TZ";
    String COUNT = "COUNT";
    String COUNT_ALL = "COUNT_ALL";
    String COMMENT = "COMMENT";
    String CURRENT_DATE = "CURRENT_DATE";
    String CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP";

    String DAY = "DAY";
    String DAY_HOUR = "DAY_HOUR";
    String DAY_MICROSECOND = "DAY_MICROSECOND";
    String DAY_MINUTE = "DAY_MINUTE";
    String DAY_SECOND = "DAY_SECOND";
    String DAYNAME = "DAYNAME";
    String DAYOFMONTH = "DAYOFMONTH";
    String DAYOFWEEK = "DAYOFWEEK";
    String DAYOFYEAR = "DAYOFYEAR";
    String DATE = "DATE";
    String DATE_ADD = "DATE_ADD";
    String DATEDIFF = "DATEDIFF";
    String DATE_SUB = "DATE_SUB";
    String DATE_FORMAT = "DATE_FORMAT";
    String DELETE_RULE = "DELETE_RULE";
    String DESC = "DESC";
    String DISTINCT = "DISTINCT";

    // ... Add all other constants in the same manner ...

    // HTTP Methods (case sensitive dont touch)
    String OPTIONS = "OPTIONS";
    String GET = "GET";
    String POST = "POST";
    String PUT = "PUT";
    String DELETE = "DELETE";

    // Only for java
    String MYSQL_TYPE = "MYSQL_TYPE";
    String JDBC_TYPE = "JDBC_TYPE";
    String MAX_LENGTH = "MAX_LENGTH";
    String AUTO_INCREMENT = "AUTO_INCREMENT";
    String SKIP_COLUMN_IN_POST = "SKIP_COLUMN_IN_POST";
    String DEFAULT_POST_VALUE = "DEFAULT_POST_VALUE";
    String REST_REQUEST_PRECOMMIT_CALLBACKS = "PRECOMMIT";
    String PRECOMMIT = REST_REQUEST_PRECOMMIT_CALLBACKS;
    String REST_REQUEST_PREPROCESS_CALLBACKS = "PREPROCESS";
    String PREPROCESS = REST_REQUEST_PREPROCESS_CALLBACKS;
    String REST_REQUEST_FINNISH_CALLBACKS = "FINISH";
    String FINISH = REST_REQUEST_FINNISH_CALLBACKS;
    //String VALIDATE_C6_ENTITY_ID_REGEX = "^" + Route.MATCH_C6_ENTITY_ID_REGEX + "$";

    String COLUMN = "COLUMN";
    String GLOBAL_COLUMN_VALIDATION = "GLOBAL_COLUMN_VALIDATION";

}
