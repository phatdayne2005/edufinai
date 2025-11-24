package vn.uth.edufinai.util;

/**
 * Constants chung cho toàn bộ application
 */
public final class Constants {

    private Constants() {
        // Utility class - prevent instantiation
    }

    /**
     * Default timeout cho WebClient calls (seconds)
     */
    public static final int DEFAULT_WEB_CLIENT_TIMEOUT_SECONDS = 5;

    /**
     * Max title length cho conversation summary
     */
    public static final int MAX_CONVERSATION_TITLE_LENGTH = 100;

    /**
     * Time constants (seconds)
     */
    public static final class Time {
        public static final int SECONDS_PER_MINUTE = 60;
        public static final int MINUTES_PER_HOUR = 60;
        public static final int HOURS_PER_DAY = 24;
        public static final int DAYS_PER_WEEK = 7;
        public static final int DAYS_PER_MONTH = 30;
        public static final int MONTHS_PER_YEAR = 12;
    }
}

