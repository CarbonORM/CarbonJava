package com.carbonorm.carbonjava;

import java.io.File;
import java.util.HashMap;

public class CarbonJAVA {

    public static final HashMap<String, Object> json = new HashMap<>();

    // Folder locations
    public static final String CARBON_ROOT = System.getProperty("user.dir") + File.separator;
    public static boolean carbon_is_root = false;
    public static String public_carbon_root = "/";
    public static String app_root;
    public static String app_view = "view/";
    public static String app_tables = System.getProperty("user.dir") + File.separator + "tables/";
    public static String reports = File.separator;
    public static String TABLE_PREFIX = "";
    private static String composer_root;

    // C6 options
    public static HashMap<String, HashMap<String, String>> configuration = new HashMap<>();
    public static String not_invoked_application = "";
    public static String not_invoked_configuration = "";
    // public static CLI commandLineInterface = null;
    // public static Application application = null;
    public static boolean setupComplete = false;



    // Application invocation method
    public static boolean is_running_production = false;
    public static boolean local = false;
    public static boolean socket = false;
    public static boolean cli = true;
    public static boolean test = false;
    public static boolean pjax = false;
    public static boolean ajax = false;
    public static boolean https = false;
    public static boolean http = false;
    public static boolean verbose = false;

    public static boolean wordpressPluginEnabled = false;

    // Validated Server Values
    public static String server_ip = "127.0.0.1";
    public static String user_ip = null;
    public static String uri;
    public static String url;
    public static String site;
    public static String protocol;

    // Don't run the application on invocation
    public static boolean safelyExit = false;

    // Basic view payload info and reporting info
    public static String site_title;
    public static String site_version;
    public static String system_email;
    public static String reply_email;


}
