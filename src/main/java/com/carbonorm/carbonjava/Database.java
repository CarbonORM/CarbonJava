package com.carbonorm.carbonjava;

import com.carbonorm.carbonjava.classes.ColorCode;
import com.carbonorm.carbonjava.interfaces.IRestMultiplePrimaryKeys;
import com.carbonorm.carbonjava.interfaces.IRestNoPrimaryKey;
import com.carbonorm.carbonjava.interfaces.IRestSinglePrimaryKey;
import com.carbonorm.carbonjava.throwables.PublicAlert;
import com.carbonorm.carbonjava.throwables.ThrowableHandler;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class Database {

    private Connection database = null;
    private Connection databaseReader = null;
    private String carbonDatabaseUsername = null;
    private String carbonDatabasePassword = null;
    private String carbonDatabaseName = null;
    private String carbonDatabasePort = "3306";
    private String carbonDatabaseHost = null;
    private String carbonDatabaseReader = null;
    private String carbonDatabaseDSN = null;
    private String carbonDatabaseReaderDSN = null;
    private String carbonDatabaseSetup = null;
    private boolean carbonDatabaseInitialized = false;
    private List<String> carbonDatabaseEntityTransactionKeys;

    private final String REMOVE_MYSQL_FOREIGN_KEY_CHECKS =
            "SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT; SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS;" +
                    "SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION; SET NAMES utf8; SET @OLD_TIME_ZONE=@@TIME_ZONE;" +
                    "SET TIME_ZONE='+00:00'; SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;" +
                    "SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;" +
                    "SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO'; SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0;";

    private final String REVERT_MYSQL_FOREIGN_KEY_CHECKS =
            "SET character_set_client = @saved_cs_client; SET TIME_ZONE=@OLD_TIME_ZONE; SET SQL_MODE=@OLD_SQL_MODE;" +
                    "SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS; SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;" +
                    "SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT; SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS;" +
                    "SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION; SET SQL_NOTES=@OLD_SQL_NOTES;";

    public Connection database(boolean reader) {
        Connection connection = reader ? databaseReader : database;
        if (connection == null) {
            return reset(reader);
        }
        try {
            Statement stmt = connection.createStatement();
            stmt.execute("SELECT 1");
            return connection;
        } catch (SQLException e) {
            // handle exception, reset connection
            return reset(reader);
        }
    }


    public static Connection reset(boolean reader) {
        // reset database connection
        // replace "your_database_url" with actual url
        // "jdbc:mysql://localhost/db?useSSL=false"
        String url = "your_database_url" + (reader ? carbonDatabaseReader : carbonDatabaseName);

        String username = carbonDatabaseUsername;

        String password = carbonDatabasePassword;

        try {

            Connection conn = DriverManager.getConnection(url, username, password);

            if (reader) {

                databaseReader = conn;

            } else {

                database = conn;

            }

            return conn;

        } catch (SQLException e) {
            // handle exception
            return null;
        }
    }

    public static boolean readerCheck(boolean reader) {
        if (reader && (carbonDatabaseReader == null || carbonDatabaseReader.isEmpty())) {
            reader = false;
        }
        return reader;
    }

    public static Connection newInstance(boolean reader) throws SQLException {
        int attempts = 0;

        reader = readerCheck(reader);

        do {
            try {
                String url = reader ? carbonDatabaseReaderDSN : carbonDatabaseDSN;

                Properties props = new Properties();

                props.setProperty("user", carbonDatabaseUsername);

                props.setProperty("password", carbonDatabasePassword);
                // add other properties as required

                Connection conn = DriverManager.getConnection(url, props);

                conn.setAutoCommit(false);  // equivalent of PDO::ATTR_PERSISTENT

                if (reader) {
                    databaseReader = conn;
                } else {
                    database = conn;
                }

                carbonDatabaseInitialized = true;

                return conn;

            } catch (SQLException e) {

                carbonDatabaseInitialized = false;

                // Log error and handle exception
                // handleSQLException(e);

            } catch (Exception e) {

                carbonDatabaseInitialized = false;

                // Log error and exit
                // handleException(e);
            } finally {
                attempts++;
            }
        } while (attempts < 3);

        String databaseType = reader ? "reader " : "";
        String message = "Failed to connect to database " + databaseType + "after (" + attempts + ") attempts.";

        // Print or log the error message
        System.err.println(message);
        System.exit(5);

        return null;  // not reachable but Java requires a return statement
    }

    public static void close(boolean reader) {
        Connection db = reader ? this.databaseReader : database;

        if (db != null) {
            try (Statement stmt = db.createStatement()) {
                // Print or log the closing message
                System.out.println("Closing MySQL connection");

                stmt.execute("KILL CONNECTION_ID();");

            } catch (SQLException e) {
                // Silently ignore the error
            }
        }

        if (reader) {
            databaseReader = null;
        } else {
            database = null;
        }
    }

    public static void setDatabase(Connection database) {
        if (database == null && Database.database != null) {
            close(false);  // close the current database connection
        }

        Database.database = database;
    }


    public static void setUp(boolean refresh, boolean cli) throws Exception {
        if (cli) {
            ColorCode.colorCode("This build is automatically generated by CarbonPHP. When you add a table be sure to re-execute the RestBuilder.", ColorCode.BACKGROUND_CYAN);
            ColorCode.colorCode("Connecting on " + carbonDatabaseDSN, ColorCode.BACKGROUND_CYAN);
        } else {
            ColorCode.colorCode("<h3>This build is automatically generated by CarbonPHP. When you add a table be sure to re-execute the <br><br><br><b>>> java -jar myapp.jar restbuilder</b></h3>"
                    + "<h1>Connecting on </h1>" + carbonDatabaseDSN + "<br>");
        }

        refreshDatabase();

        if (refresh && !cli) {
            System.out.println("<br><br><h2>Refreshing in 6 seconds</h2><script>let t1 = window.setTimeout(function(){ window.location.href = '/'; },6000);</script>");
            System.exit(0);
        }

        // assuming you have a `database` method that returns a Connection
        Connection dbConnection = database(false);

        // use dbConnection as needed
    }


    public static boolean verify() throws PublicAlert {
        Connection connection = database(false);

        try {
            if (connection.getAutoCommit()) {
                return true;
            }

            connection.rollback();

            if (!carbonDatabaseEntityTransactionKeys.isEmpty()) {
                for (String key : carbonDatabaseEntityTransactionKeys) {
                    removeEntity(key);
                }
            }
        } catch (SQLException e) {
            ThrowableHandler.generateLog(e);
        }

        return false;
    }

    private static int committedTransactions = 0;

    public static boolean commit() throws PublicAlert {
        String thisMethod = Thread.currentThread().getStackTrace()[1].getMethodName();

        Connection db = database(false);
        try {
            if (!db.getAutoCommit()) {
                db.commit();
                committedTransactions++;
                comment(thisMethod, null);
                return true;
            } else {
                return true;
            }
        } catch (SQLException e) {
            // rollback
            return verify();
        }
    }

    protected static String beginTransaction(String tagId, String dependant) throws SQLException {
        Connection db = MyDatabaseClass.database(false);

        String key = MyDatabaseClass.newEntity(tagId, dependant);

        if (db.getAutoCommit()) {
            db.setAutoCommit(false);
        }

        return key;
    }

    public static String new_entity(String tag_id, String dependant) throws Exception {
        int count = 0;
        String id;
        do {
            count++;
            PreparedStatement stmt = database(false).prepareStatement("INSERT INTO Carbons (ENTITY_TAG, ENTITY_FK) VALUES (?, ?)");
            stmt.setString(1, tag_id);
            stmt.setString(2, dependant);
            int result = stmt.executeUpdate();

            if (result > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    id = rs.getString(1);
                } else {
                    throw new Exception("Failed to retrieve generated key.");
                }
            } else {
                id = null;
            }

        } while (id == null && count < 4);  // todo - why four?

        if (id == null) {
            throw new Exception("Failed to create a new entity.");
        }

        carbonDatabaseEntityTransactionKeys.add(id);

        return id;
    }

    public static boolean remove_entity(String id) throws SQLException {
        PreparedStatement stmt = database(false).prepareStatement("DELETE FROM Carbons WHERE entity_pk = ?");
        stmt.setString(1, id);
        return stmt.executeUpdate() > 0;
    }

    public static boolean execute(String sql, Object... execute) throws SQLException {


        PreparedStatement stmt = database(!isWriteQuery(sql)).prepareStatement(sql);

        for (int i = 0; i < execute.length; i++) {
            stmt.setObject(i + 1, execute[i]);
        }

        return stmt.executeUpdate() > 0;

    }

    /**
     * Use prepared statements with question mark values.
     * <p>
     * Pass parameters separated by commas in order denoted by the SQL stmt.
     * <p>
     * Example:
     * List<Map<String, Object>> data = MyClass.fetch("SELECT * FROM user WHERE user_id = ?", id);
     *
     * @param sql     The SQL query to execute.
     * @param execute The values to bind to the query.
     * @return The result set of the query as a List of Maps.
     */
    protected static List<Map<String, Object>> fetch(String sql, Object... execute) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            boolean reader = !isWriteQuery(sql);

            Connection connection = database(reader);
            PreparedStatement stmt = connection.prepareStatement(sql);

            // Bind parameters
            for (int i = 0; i < execute.length; i++) {
                stmt.setObject(i + 1, execute[i]);
            }

            // Execute query
            if (stmt.execute()) {
                ResultSet rs = stmt.getResultSet();
                ResultSetMetaData rsmd = rs.getMetaData();

                // Convert ResultSet to List of Maps
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                        row.put(rsmd.getColumnName(i), rs.getObject(i));
                    }
                    result.add(row);
                }
            }

        } catch (SQLException e) {
            // Handle exception
            e.printStackTrace();
            System.exit(1);
        }

        // Return single element if result only contains one row, else return all rows
        return (result.size() == 1) ? Collections.singletonList(result.get(0)) : result;
    }

    public static List<Map<String, Object>> fetchAll(String sql, Object... execute) throws SQLException {

        PreparedStatement stmt = database(true).prepareStatement(sql);

        for (int i = 0; i < execute.length; i++) {
            stmt.setObject(i + 1, execute[i]);
        }
        ResultSet rs = stmt.executeQuery();

        List<Map<String, Object>> resultList = new ArrayList<>();
        ResultSetMetaData md = rs.getMetaData();
        int columns = md.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new HashMap<>(columns);
            for (int i = 1; i <= columns; ++i) {
                row.put(md.getColumnName(i), rs.getObject(i));
            }
            resultList.add(row);
        }

        return resultList;
    }


    /**
     * Determine the likelihood that this query could alter anything
     * <p>
     * Statements are considered read-only when:
     * 1. not including UPDATE nor other "may-be-write" strings
     * 2. begin with SELECT etc.
     *
     * @param q Query.
     * @return boolean
     * @since 1.0.0
     */
    public static boolean isWriteQuery(String q) {
        // Trim potential whitespace or subquery chars
        q = q.trim();

        // Possible writes
        Pattern writePattern = Pattern.compile("(?:^|\\s)(?:ALTER|CREATE|ANALYZE|CHECK|OPTIMIZE|REPAIR|CALL|DELETE|DROP|INSERT|LOAD|REPLACE|UPDATE|SHARE|SET|RENAME\\s+TABLE)(?:\\s|$)", Pattern.CASE_INSENSITIVE);
        if (writePattern.matcher(q).find()) {
            return true;
        }

        // Not possible non-writes
        Pattern readPattern = Pattern.compile("^(?:SELECT|SHOW|DESCRIBE|DESC|EXPLAIN)(?:\\s|$)", Pattern.CASE_INSENSITIVE);
        return !readPattern.matcher(q).find();
    }

    // Fetch a single column
    public static List<Object> fetchColumn(String sql, Object... execute) {
        List<Object> result = new ArrayList<>();
        try {
            boolean reader = !isWriteQuery(sql);
            Connection connection = database(reader);
            PreparedStatement stmt = connection.prepareStatement(sql);

            // Bind parameters
            for (int i = 0; i < execute.length; i++) {
                stmt.setObject(i + 1, execute[i]);
            }

            // Execute query and fetch the single column
            if (stmt.execute()) {
                ResultSet rs = stmt.getResultSet();
                while (rs.next()) {
                    result.add(rs.getObject(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return result;
    }

    protected static void runRefreshSchema(Callable<Boolean>[] REFRESH_SCHEMA) {
        try {
            for (Callable<Boolean> validation : REFRESH_SCHEMA) {
                Boolean result = validation.call();

                if (!result) {
                    throw new RuntimeException("Any method used in REFRESH_SCHEMA must not return false. Method invocation failed.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Replace with your own error handling
        }

    }

    public static void scanAnd(Consumer<Class<?>> callback, String tableDirectory) throws Exception {

        if (tableDirectory == null) {

            tableDirectory = Rest.autoTargetTableDirectory();

        }

        File dir = new File(tableDirectory);

        File[] restful = dir.listFiles((dir1, name) -> name.toLowerCase().endsWith(".java"));

        assert restful != null;

        for (File file : restful) {
            String className = file.getName().replace(".java", "");
            try {
                Class<?> cls = Class.forName(className);

                if (!Rest.class.isAssignableFrom(cls)) {
                    System.out.println("\n\nThe class '" + cls.getName() + "' does not implement Rest." +
                            " This would indicate a custom class in the table's namespaced directory. Please avoid doing this.\n");
                    continue;
                }

                if (!IRestMultiplePrimaryKeys.class.isAssignableFrom(cls)
                        && !IRestSinglePrimaryKey.class.isAssignableFrom(cls)
                        && !IRestNoPrimaryKey.class.isAssignableFrom(cls)) {

                    System.out.println("The table (" + cls.getName() + ") did not interface the required (iRestMultiplePrimaryKeys, iRestSinglePrimaryKey, or iRestNoPrimaryKey). This is unexpected.");

                    continue;

                }

                callback.accept(cls);
            } catch (ClassNotFoundException e) {
                System.out.println("\n\nCouldn't load the class '" + className + "' for refresh. This may indicate your file " +
                        "contains a syntax error or is not generated by the restful API.\n");
            }
        }
    }

    public static void tryCatchSQLException(SQLException e) {

        HashMap<String, Object> logArray = ThrowableHandler.generateLog(e, true);

        // Note that Java doesn't have a direct equivalent of PHP closures with 'use',
        // so we need to pass our required variables as parameters
        Runnable throwableHandler = new Runnable() {
            @Override
            public void run() {
                try {
                    ThrowableHandler.exitAndSendBasedOnRequested(logArray, ThrowableHandler.generateBrowserReport(logArray, true));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                System.exit(32);
            }
        };

        switch (e.getErrorCode()) { // Database has not been created
            case 0:
                throwableHandler.run(); // this terminates
                break;
            case 1049: // 'HY000' error code in MySQL is 0 in Java, 1049 is 'Unknown database'
                createDatabaseIfNotExist();
                refreshDatabase();
                throwableHandler.run();
                break;
            case 42: // '42S02' error code is 42 in Java
                throwableHandler.run();
                //setUp(!CarbonJAVA.cli, CarbonJAVA.cli);
                break;
            default:
                if (Database.carbonDatabaseUsername.isEmpty()) {
                    logArray.put("ISSUE_NO_DB_USER","<h2>You must set a database username. See CarbonPHP.com for documentation</h2>");
                }
                if (Database.carbonDatabasePassword.isEmpty()) {
                    logArray.put("ISSUE_NO_DB_PASS","<h2>You may need to set a database password. See CarbonPHP.com for documentation</h2>");
                }
                throwableHandler.run();
        }

        System.exit(1);
    }

    public static void createDatabaseIfNotExist() {

        try {
            // Assume carbonDatabaseDSN is like "jdbc:mysql://localhost:3306/dbname"
            // Split the URL to get the database name
            String[] urlParts = carbonDatabaseDSN.split("/");
            String dbName = urlParts[urlParts.length - 1];

            if (dbName.isEmpty()) {
                throw new SQLException("Failed to parse the database name. Please look at the mysql connection information.");
            }

            // Remove the database name from the connection string to connect without specifying the database
            String connectionUrl = carbonDatabaseDSN.substring(0, carbonDatabaseDSN.lastIndexOf("/"));

            // Connect to the database
            try (Connection conn = DriverManager.getConnection(connectionUrl, carbonDatabaseUsername, carbonDatabasePassword)) {
                // Create the database if it does not exist
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("CREATE DATABASE IF NOT EXISTS " + dbName);
                    stmt.execute("USE " + dbName);
                }
            }
        } catch (SQLException e) {

            ThrowableHandler.generateLog(e);

        }

    }

    /**
     * This function, scanAndRunRefreshDatabase, performs a number of operations related to database management, particularly for updating and refreshing the schema of the database. Here's a breakdown of what this function is doing:
     * Scanning: The function begins by scanning through files in a directory specified by the $tableDirectory parameter. It's expecting these files to contain PHP class definitions that represent tables in a database. These classes should define a series of constants such as TABLE_NAME, CREATE_TABLE_SQL, and COLUMNS, among others.
     * Validating: Each class file is validated to ensure it contains the required constants. If any required constants are missing, an error message is displayed and the process exits with an error code.
     * Refreshing: For each valid class, a series of database operations are performed. These include creating the table (if it doesn't exist), adding columns to the table (as defined by the COLUMNS constant), and updating column types.
     * Database Transactions: If a database transaction is active, the function attempts to commit it. If the commit fails, an error message is displayed and the process exits with an error code.
     * Validation After Refresh: After all operations have been completed, the function validates the newly created or modified tables. It does this by comparing the MySQL dump (i.e., the current state of the database) to the SQL statements generated from the PHP classes. If there are any discrepancies, error messages are displayed, the process exits with an error code, and manual intervention may be required.
     * Cleanup: If a MySQL dump file exists at the end of the function, it is deleted.
     * In summary, this function is a utility to manage a database schema based on a series of PHP classes that represent tables. This includes creating and updating tables, as well as ensuring the database schema matches the definitions provided by the PHP classes.
     *
     */
    public static void refreshDatabase(String tableDirectory) {
        File dir = new File(tableDirectory);

        if(!dir.exists() || !dir.isDirectory()) {

            System.out.println("Provided path is not a directory.");

            return;

        }

        // This filter only returns PHP files
        FilenameFilter fileFilter = (dir1, name) -> name.endsWith(".java");

        File[] files = dir.listFiles(fileFilter);

        assert files != null;

        for(File file : files) {

            String className = file.getName().replace(".java", "");

            try {
                Class<?> clazz = Class.forName(className);

                Field tableNameField = clazz.getField("TABLE_NAME");
                String tableName = (String) tableNameField.get(null);

                Field createTableSqlField = clazz.getField("CREATE_TABLE_SQL");
                String createTableSql = (String) createTableSqlField.get(null);

                execute(createTableSql, null);

            } catch (ClassNotFoundException e) {
                System.out.println("Class not found: " + className);
            } catch (NoSuchFieldException e) {
                System.out.println("Field not found in class: " + className);
            } catch (IllegalAccessException e) {
                System.out.println("Illegal access to field in class: " + className);
            }
        }
    }

}