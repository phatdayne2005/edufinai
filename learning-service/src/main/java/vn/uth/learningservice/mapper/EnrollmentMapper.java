package vn.uth.learningservice.mapper;

import org.mapstruct.*;
import vn.uth.learningservice.dto.response.EnrollmentRes;
import vn.uth.learningservice.model.*;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {
    @Mapping(target = "learnerId", expression = "java(e.getLearner() == null ? null : e.getLearner().getId())")
    @Mapping(target = "lessonId", expression = "java(e.getLesson() == null ? null : e.getLesson().getId())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    EnrollmentRes toRes(Enrollment e);
}