package vn.uth.edufinai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.uth.edufinai.entity.Permission;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {}
