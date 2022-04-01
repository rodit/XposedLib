package xyz.rodit.xposed.services;

public class Messages {

    public static final int GET_CONFIG = 1;
    public static final int CONFIG_VALUE = 2;
    public static final int GET_MAPPINGS = 3;
    public static final int MAPPINGS_VALUE = 4;
    public static final int MAPPINGS_FAILED = 5;

    public static final int DOWNLOAD_FILE = 100;

    public static final String MAPPINGS_BUILD_NAME = "build";
    public static final String MAPPINGS_VALUE_NAME = "mappings";

    public static final class DownloadRequest {

        public static final String USE_DOWNLOAD_MANAGER = "useDownloadManager";
        public static final String SOURCE = "source";
        public static final String DESTINATION = "destination";
        public static final String TITLE = "title";
        public static final String DESCRIPTION = "description";
    }
}
