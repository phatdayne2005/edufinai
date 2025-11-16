package vn.uth.edufinai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.uth.edufinai.entity.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {}
