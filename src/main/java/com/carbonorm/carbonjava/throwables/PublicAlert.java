package com.carbonorm.carbonjava.throwables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PublicAlert extends Exception {

    private static Map<String, Object> json = new HashMap<>();

    public static boolean alertSet() {
        Map<String, Object> alert = (Map<String, Object>) json.get("alert");
        if (alert != null) {
            return alert.containsKey("danger") || alert.containsKey("error") || alert.containsKey("input");
        }
        return false;
    }

    public static void jsonAlert(String message, String title, String type, String icon, int status, boolean intercept, boolean stack) {
        if (stack) {
            Map<String, Object> previousJson = new HashMap<>(json);
            json.clear();
            json.put("previous_json", previousJson);
            json.put("sql", new ArrayList<>());
            json.put("status", status);
        } else {
            json.put("status", status);
        }

        if (!type.matches("default|info|success|warning|danger|error|input|custom")) {
            message += " The React Alert type given " + type + " is not supported.";
            type = "danger";
        }

        if (title == null) {
            title = "Danger!";
        }

        Map<String, Object> alert = new HashMap<>();
        alert.put("message", message);
        alert.put("type", type);
        alert.put("title", title);
        alert.put("icon", icon);
        alert.put("intercept", intercept);

        ArrayList<Map<String, Object>> alerts = (ArrayList<Map<String, Object>>) json.get("alert");
        if (alerts == null) {
            alerts = new ArrayList<>();
            json.put("alert", alerts);
        }
        alerts.add(alert);
    }

    private static void alert(String message, String code) {
        if (message.charAt(message.length() - 1) == '.' && !"success".equals(code) && !"info".equals(code)) {
            message += " Contact us if problem persists.";
        }
        Map<String, ArrayList<String>> alerts = (Map<String, ArrayList<String>>) json.get("alert");
        if (alerts == null) {
            alerts = new HashMap<>();
            json.put("alert", alerts);
        }
        ArrayList<String> messages = alerts.computeIfAbsent(code, k -> new ArrayList<>());
        messages.add(message);
    }

    public static void success(String message) {
        alert(message, "success");
    }

    public static void info(String message) {
        alert(message, "info");
    }

    public static void danger(String message) {
        alert(message, "danger");
    }

    public static void warning(String message) {
        alert(message, "warning");
    }

    public PublicAlert(String message) {
        super(message);
        if (message == null || message.isEmpty()) {
            message = "Whoa, a Public Alert was thrown without a message attached. This is awful.";
            alert(message, "error");
        }
    }
}
