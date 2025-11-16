package vn.uth.edufinai.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import vn.uth.edufinai.dto.request.RoleRequest;
import vn.uth.edufinai.dto.response.RoleResponse;
import vn.uth.edufinai.entity.Role;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleRequest request);

    RoleResponse toRoleResponse(Role role);
}
