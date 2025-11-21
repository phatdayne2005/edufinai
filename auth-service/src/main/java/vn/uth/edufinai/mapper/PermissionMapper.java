package vn.uth.edufinai.mapper;

import org.mapstruct.Mapper;

import vn.uth.edufinai.dto.request.PermissionRequest;
import vn.uth.edufinai.dto.response.PermissionResponse;
import vn.uth.edufinai.entity.Permission;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission(PermissionRequest request);

    PermissionResponse toPermissionResponse(Permission permission);
}
