package vn.uth.edufinai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.uth.edufinai.entity.InvalidatedToken;

@Repository
public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, String> {}
