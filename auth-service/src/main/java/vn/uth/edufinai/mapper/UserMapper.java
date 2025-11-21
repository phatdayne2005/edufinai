package vn.uth.edufinai.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import vn.uth.edufinai.dto.request.UserCreationRequest;
import vn.uth.edufinai.dto.request.UserUpdateRequest;
import vn.uth.edufinai.dto.response.UserResponse;
import vn.uth.edufinai.entity.User;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {
    User toUser(UserCreationRequest request);

    UserResponse toUserResponse(User user);

    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "password", ignore = true)  // Ignore password - will be handled separately in service
    @Mapping(target = "id", ignore = true)  // Ignore id - should not be updated
    @Mapping(target = "username", ignore = true)  // Ignore username - should not be updated
    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}
