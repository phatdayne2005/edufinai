package vn.uth.edufinai.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.sql.Date;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Converter để convert ZonedDateTime <-> DATE trong database
 * Vì report_date trong DB là DATE type, nhưng Entity dùng ZonedDateTime
 * Luôn sử dụng UTC timezone để đảm bảo nhất quán
 */
@Slf4j
@Converter(autoApply = false)
public class ZonedDateTimeAttributeConverter implements AttributeConverter<ZonedDateTime, Date> {

    private static final ZoneId UTC = ZoneId.of("UTC");

    @Override
    public Date convertToDatabaseColumn(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }
        try {
            // Convert về UTC trước khi lấy LocalDate
            ZonedDateTime utcDateTime = zonedDateTime.withZoneSameInstant(UTC);
            Date result = Date.valueOf(utcDateTime.toLocalDate());
            log.debug("Converting ZonedDateTime to Date: {} -> {}", zonedDateTime, result);
            return result;
        } catch (Exception e) {
            log.error("Error converting ZonedDateTime to Date: {}", zonedDateTime, e);
            throw new RuntimeException("Failed to convert ZonedDateTime to Date: " + e.getMessage(), e);
        }
    }

    @Override
    public ZonedDateTime convertToEntityAttribute(Date date) {
        if (date == null) {
            return null;
        }
        try {
            // java.sql.Date không hỗ trợ toInstant(), cần dùng toLocalDate() rồi atStartOfDay()
            // Luôn trả về UTC timezone với time = 00:00:00
            ZonedDateTime result = date.toLocalDate().atStartOfDay(UTC);
            log.debug("Converting Date to ZonedDateTime: {} -> {}", date, result);
            return result;
        } catch (Exception e) {
            log.error("Error converting Date to ZonedDateTime: {}", date, e);
            throw new RuntimeException("Failed to convert Date to ZonedDateTime: " + e.getMessage(), e);
        }
    }
}

