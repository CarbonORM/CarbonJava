package com.carbonorm.carbonjava.throwables;

import com.carbonorm.carbonjava.CarbonJAVA;
import com.carbonorm.carbonjava.classes.ColorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ThrowableHandler {

    public static final String LOG_ARRAY = "LOG_ARRAY";
    public static final String HTML_ERROR_PAGE = "HTML_ERROR_PAGE";
    public static final String STORED_HTML_LOG_FILE_PATH = "STORED_HTML_LOG_FILE_PATH";
    public static final String STORAGE_LOCATION_KEY = "THROWABLE STORED TO FILE";
    public static final String TRACE = "TRACE";
    public static final String GLOBALS_JSON = "$GLOBALS['json']";
    public static final String INNODB_STATUS = "INNODB_STATUS";
    public static final String DEBUG_BACKTRACE = "debug_backtrace()";

    // TODO: defaultLocation this does nothing.
    public static String defaultLocation = null;

    /**
     * Determine if a generated error log should be shown on the browser.
     * This value can be set using the ["ERROR"]["SHOW"] configuration option
     */
    public static boolean printToScreen = true;

    public static boolean bypassStandardJavaErrorHandler = true;
    public static boolean storeReport = false;

    public enum ThrowableReportDisplay {
        FULL_DEFAULT,
        CLI_MINIMAL
    }

    public static ThrowableReportDisplay throwableReportDisplay = ThrowableReportDisplay.FULL_DEFAULT;

    public static String fileName = "";
    public static String className = "";
    public static String methodName = "";

    // The following two should be of type   ?Closure|array
    // @link https://www.php.net/manual/en/function.set-error-handler.php
    public static Object oldErrorHandler = null;
    public static Object oldExceptionHandler = null;

    public static Integer oldErrorLevel = null;

    public static boolean attemptRestartAfterError = false;


    public static void generateLog(Throwable e) {
        generateLog(e, false);
    }
    public static HashMap<String, Object> generateLog(Throwable e, boolean shouldReturn) {
        if (e != null) {
            try {
                throw new PublicAlert("generateLog was called with e = null");
            } catch (PublicAlert error) {
                e = error;
            }

        }

        ColorCode.colorCode("Generating pretty error message.", ColorCode.CYAN);

        assert e != null;

        ColorCode.colorCode("\t" + e.getClass().getName(), ColorCode.RED);
        ColorCode.colorCode("\t\t" + e.getMessage(), ColorCode.RED);

        StackTraceElement[] stackTraceElements = e.getStackTrace();

        HashMap<String, Object> logMap = new HashMap<>();

        logMap.put("ERROR TYPE", "A Public Alert Was Thrown!");
        logMap.put("MESSAGE", e.getMessage());
        logMap.put("CAUSE", e.getCause());
        logMap.put("CLASS", e.getClass().getName());
        logMap.put("STACK TRACE", Arrays.toString(e.getStackTrace()));

        // todo - handle with json & html responses

        for (Map.Entry<String, Object> entry : logMap.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

        if (shouldReturn) {

            return logMap;

        }

        System.exit(1);

        return null;

    }

    public static String getErrorTemplate() {
        String content = "";
        try (InputStream inputStream = ThrowableHandler.class.getResourceAsStream("./errorTemplate.hbs");
             Scanner scanner = new Scanner(inputStream, "UTF-8")) {
            content = scanner.useDelimiter("\\A").next();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return content;
    }

    private static ObjectMapper mapper = new ObjectMapper();
    private static MustacheFactory mf = new DefaultMustacheFactory();

    protected static String errorTemplate(Map<String, Object> message, int code) {
        String cleanErrorReport = "";

        String actualMessage = message.keySet().iterator().next();

        for (Map.Entry<String, Object> entry : message.entrySet()) {
            String left = entry.getKey();
            Object right = entry.getValue();

            boolean blocks = left.equals(TRACE)
                    || left.equals(GLOBALS_JSON)
                    || left.equals(DEBUG_BACKTRACE)
                    || left.equals(INNODB_STATUS);

            if (blocks) {
                right = jsonEncodeAndWrapForHTML(right);
            }

            if (right instanceof Map) {
                right = jsonEncodeAndWrapForHTML(right);
            }

            cleanErrorReport += blocks ?
                    "<p>> <span>" + left + "</span>: <i>" + right + "</i></p>"
                    :
                    "<p>> <span>" + left + "</span>: <b style=\"color: white\">\"</b><i>" + right + "</i><b style=\"color: white\">\"</b></p>";
        }


        String statusText = statusText(code);

        String publicRoot = CarbonJAVA.public_carbon_root.trim();
        if (!publicRoot.isEmpty()) {
            publicRoot = "/" + publicRoot;
        }

        Map<String, Object> scopes = new HashMap<>();
        scopes.put("carbon_public_root", publicRoot);
        scopes.put("public_root", CarbonJAVA.public_carbon_root.trim());
        scopes.put("code", code);
        scopes.put("statusText", statusText);
        scopes.put("actual_message", actualMessage);
        scopes.put("actual_message_body", message.get(actualMessage));
        scopes.put("cleanErrorReport", cleanErrorReport);
        scopes.put("json", CarbonJAVA.json);
        try {
            scopes.put("json_string", mapper.writeValueAsString(CarbonJAVA.json));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        StringWriter writer = new StringWriter();

        mf.compile(getErrorTemplate()).execute(writer, scopes);

        return writer.toString();
    }


    public static void checkCreateLogFile(StringBuilder message) throws IOException, PublicAlert {
        Path directory = Paths.get(defaultLocation).getParent();

        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                throw new PublicAlert("The directory (" + directory + ") for ThrowableHandler.defaultLocation (" + defaultLocation + ") does not exist and could not be created");
            }
        }

        Path logFile = Paths.get(defaultLocation);

        if (!Files.exists(logFile)) {
            try {
                Files.createFile(logFile);
            } catch (IOException e) {
                message.append("\n\nCould not create file (").append(defaultLocation).append(") as it does not exist on the system. All folders appear correct. Please create the directories required to store logs correctly!\n");
            }
        }
    }

    public static List<String> grabFileSnippet(String file, int line, boolean raw) throws IOException {
        Path filePath = Paths.get(file);
        if (Files.exists(filePath)) {
            List<String> source = Files.readAllLines(filePath);
            int startLine = line - 10;
            if (startLine < 0) startLine = 0;  // In Java, negative index would throw an exception
            List<String> snippet = source.subList(startLine, startLine + 20);
            if (raw) {
                return snippet;
            }
            // The highlight function is not available in standard Java
            // We assume that there is a custom highlight function
            // String highlighted = highlight(String.join("\n", snippet));
            // return highlighted;
            return snippet;  // return raw snippet as highlight function is not available
        }
        return List.of("");  // Return list with empty string if file doesn't exist
    }

    public static String jsonEncodeAndWrapForHTML(Object code) {
        if (!(code instanceof String)) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                code = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(code);
            } catch (JsonProcessingException e) {
                try {
                    code = Arrays.toString((Object[]) code);
                } catch (Exception ex) {
                    // ColorCode.colorCode("The trace failed to be json_encoded, serialized, or printed with print_r().", ColorCode.RED);
                    // ColorCode.colorCode(e.getMessage(), ColorCode.RED);
                    code = "** PARSING FAILED **";
                }
            }
        }
        return "<pre>" + code + "</pre>";
    }

    public static boolean shouldSendJson(HttpServletRequest request) {
        String contentType = request.getContentType();
        String requestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(requestedWith)
                || (contentType != null && contentType.contains("application/json"));
    }

    public static String statusText(int code) {
        Map<Integer, String> statusCodes = new HashMap<>();
        statusCodes.put(100, "Continue");
        statusCodes.put(101, "Switching Protocols");
        statusCodes.put(200, "OK");
        statusCodes.put(201, "Created");
        statusCodes.put(202, "Accepted");
        statusCodes.put(203, "Non-Authoritative Information");
        statusCodes.put(204, "No Content");
        statusCodes.put(205, "Reset Content");
        statusCodes.put(206, "Partial Content");
        statusCodes.put(300, "Multiple Choices");
        statusCodes.put(302, "Found");
        statusCodes.put(303, "See Other");
        statusCodes.put(304, "Not Modified");
        statusCodes.put(305, "Use Proxy");
        statusCodes.put(400, "Bad Request");
        statusCodes.put(401, "Unauthorized");
        statusCodes.put(402, "Payment Required");
        statusCodes.put(403, "Forbidden");
        statusCodes.put(404, "Not Found");
        statusCodes.put(405, "Method Not Allowed");
        statusCodes.put(406, "Not Acceptable");
        statusCodes.put(407, "Proxy Authentication Required");
        statusCodes.put(408, "Request Timeout");
        statusCodes.put(409, "Conflict");
        statusCodes.put(410, "Gone");
        statusCodes.put(411, "Length Required");
        statusCodes.put(412, "Precondition Failed");
        statusCodes.put(413, "Request Entity Too Large");
        statusCodes.put(414, "Request-URI Too Long");
        statusCodes.put(415, "Unsupported Media Type");
        statusCodes.put(416, "Requested Range Not Satisfiable");
        statusCodes.put(417, "Expectation Failed");
        statusCodes.put(500, "Internal Server Error");
        statusCodes.put(501, "Not Implemented");
        statusCodes.put(502, "Bad Gateway");
        statusCodes.put(503, "Service Unavailable");
        statusCodes.put(504, "Gateway Timeout");
        statusCodes.put(505, "HTTP Version Not Supported");

        return statusCodes.getOrDefault(code, null);
    }


    public static void closeAndExit(int exitCode) {
        System.out.flush();
        System.err.flush();

        // Exit the application
        System.exit(exitCode);
    }

    public static void generateBrowserReport(HashMap<String, Object> errorForTemplate) {
        generateBrowserReport(errorForTemplate, false);
    }
    public static String generateBrowserReport(HashMap<String, Object> errorForTemplate, boolean shouldReturn) {

        if (!errorForTemplate.containsKey("CODE")) {

            errorForTemplate.put("CODE", "0");

        }

        int code;

        try {

            code = Integer.parseInt(errorForTemplate.get("CODE").toString());

        } catch (NumberFormatException e) {

            code = 400;

        }

        String errorPage = errorTemplate(errorForTemplate, code);

        if (shouldReturn) {

            return errorPage;

        }

        if (!CarbonJAVA.setupComplete) {

            exitAndSendBasedOnRequested(errorForTemplate, errorPage);

        }

        exitAndSendBasedOnRequested(errorForTemplate, errorPage);

        return null;  // This line will never be reached, but is necessary to satisfy the compiler

    }

    public static void exitAndSendBasedOnRequested(HashMap<String, Object> json, String html) {
        try {

            ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            HttpServletResponse response = sra.getResponse();

            String contentType;

            boolean sendJson = shouldSendJson(null);

            int code = json.containsKey("CODE") && json.get("CODE") instanceof Number ? (int) json.get("CODE") : 400;

            if (code < 100 || code > 599) {
                code = 400;
            }

            contentType = sendJson ? "application/json" : "text/html";

            response.setContentType(contentType);

            response.setStatus(code);

            PrintWriter writer = response.getWriter();

            if (sendJson) {

                ObjectMapper objectMapper = new ObjectMapper();

                String jsonString = null;

                jsonString = objectMapper.writeValueAsString(json);

                writer.println(jsonString);

            } else {

                writer.println(html != null ? html : generateBrowserReport(json, true));

            }

            writer.flush();

            writer.close();

            System.exit(1);

        } catch (IOException e) {
            ThrowableHandler.generateLog(e, false);
        }

    }

}
