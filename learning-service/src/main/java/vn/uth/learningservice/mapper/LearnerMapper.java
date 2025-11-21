package vn.uth.learningservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.uth.learningservice.dto.response.LearnerRes;
import vn.uth.learningservice.dto.shared.LearnerLevel;
import vn.uth.learningservice.model.Learner;

@Mapper(componentModel = "spring")
public interface LearnerMapper {
    
    @Mapping(target = "level", expression = "java(toDtoLevel(learner.getLevel()))")
    LearnerRes toDto(Learner learner);
    
    default LearnerLevel toDtoLevel(Learner.Level modelLevel) {
        if (modelLevel == null) return null;
        return LearnerLevel.valueOf(modelLevel.name());
    }
    
    default Learner.Level toModelLevel(LearnerLevel dtoLevel) {
        if (dtoLevel == null) return null;
        return Learner.Level.valueOf(dtoLevel.name());
    }
}

