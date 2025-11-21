package vn.uth.learningservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.uth.learningservice.dto.response.CreatorRes;
import vn.uth.learningservice.model.Creator;

@Mapper(componentModel = "spring")
public interface CreatorMapper {
    
    @Mapping(target = "totalLessons", ignore = true)
    CreatorRes toDto(Creator creator);
}

