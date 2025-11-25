package vn.uth.learningservice.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;
import vn.uth.learningservice.dto.request.LessonCreateReq;
import vn.uth.learningservice.dto.request.LessonUpdateReq;
import vn.uth.learningservice.dto.response.LessonRes;
import vn.uth.learningservice.model.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Mapper(componentModel = "spring")
public interface LessonMapper {

    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "moderator", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enrollments", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "totalQuestions", ignore = true)
    @Mapping(target = "commentByMod", ignore = true)
    @Mapping(target = "quizJson", source = "quizJson", qualifiedByName = "jsonNodeToString")
    Lesson toEntity(LessonCreateReq req, @Context ObjectMapper objectMapper);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enrollments", ignore = true)
    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "moderator", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalQuestions", ignore = true)
    @Mapping(target = "quizJson", source = "quizJson", qualifiedByName = "jsonNodeToString")
    void patch(@MappingTarget Lesson entity, LessonUpdateReq req, @Context ObjectMapper objectMapper);

    @Mapping(target = "creatorId", source = "creator.id")
    @Mapping(target = "moderatorId", source = "moderator.id")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "localDateTimeToInstant")
    @Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = "localDateTimeToInstant")
    @Mapping(target = "publishedAt", source = "publishedAt", qualifiedByName = "localDateTimeToInstant")
    @Mapping(target = "quizJson", source = "quizJson", qualifiedByName = "stringToJsonNode")
    LessonRes toRes(Lesson entity, @Context ObjectMapper objectMapper);

    @Named("localDateTimeToInstant")
    default Instant localDateTimeToInstant(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    @Named("jsonNodeToString")
    default String jsonNodeToString(JsonNode jsonNode, @Context ObjectMapper objectMapper) {
        if (jsonNode == null) {
            return null;
        }
        // Nếu là TextNode (client gửi chuỗi JSON), trả về nội dung chuỗi
        if (jsonNode.isTextual()) {
            return jsonNode.asText();
        }
        try {
            return objectMapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting JsonNode to String", e);
        }
    }

    @Named("stringToJsonNode")
    default JsonNode stringToJsonNode(String jsonString, @Context ObjectMapper objectMapper) {
        if (jsonString == null || jsonString.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(jsonString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting String to JsonNode", e);
        }
    }
}
