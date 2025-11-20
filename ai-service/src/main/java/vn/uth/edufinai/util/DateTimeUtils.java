package vn.uth.edufinai.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Utility class cho date/time operations
 */
public final class DateTimeUtils {

    public static final ZoneId UTC = ZoneId.of("UTC");

    private DateTimeUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Lấy current time ở UTC timezone
     * 
     * @return ZonedDateTime hiện tại ở UTC
     */
    public static ZonedDateTime nowUtc() {
        return ZonedDateTime.now(UTC);
    }

    /**
     * Normalize ZonedDateTime về UTC timezone
     * 
     * @param dateTime ZonedDateTime cần normalize
     * @return ZonedDateTime ở UTC timezone
     */
    public static ZonedDateTime toUtc(ZonedDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.withZoneSameInstant(UTC);
    }

    /**
     * Kiểm tra và normalize ZonedDateTime về UTC, fallback về now nếu null hoặc invalid
     * 
     * @param dateTime ZonedDateTime cần validate
     * @param fallbackToNow Nếu true, sẽ fallback về now() nếu null hoặc invalid
     * @return ZonedDateTime đã được normalize
     */
    public static ZonedDateTime normalizeToUtc(ZonedDateTime dateTime, boolean fallbackToNow) {
        if (dateTime == null) {
            return fallbackToNow ? nowUtc() : null;
        }

        if (dateTime.toEpochSecond() <= 0) {
            return fallbackToNow ? nowUtc() : dateTime;
        }

        return toUtc(dateTime);
    }
}

