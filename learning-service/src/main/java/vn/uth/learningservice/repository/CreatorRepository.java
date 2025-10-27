package vn.uth.learningservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.uth.learningservice.model.Creator;

import java.util.UUID;

@Repository
public interface CreatorRepository extends JpaRepository<Creator, UUID> {
}
