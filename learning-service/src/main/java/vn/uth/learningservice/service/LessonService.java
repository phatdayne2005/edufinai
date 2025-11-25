package vn.uth.learningservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.learningservice.model.Lesson;
import vn.uth.learningservice.model.Moderator;
import vn.uth.learningservice.repository.LessonRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepo;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public Lesson getById(UUID id) {
        return lessonRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + id));
    }

    private int calculateTotalQuestions(String quizJson) {
        if (quizJson == null || quizJson.isBlank()) {
            return 0;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(quizJson);
            if (root.has("questions")) {
                return root.get("questions").size();
            }
        } catch (Exception e) {
            // ignore or log
        }
        return 0;
    }

    public Optional<Lesson> findBySlug(String slug) {
        return lessonRepo.findBySlug(slug);
    }

    public List<Lesson> listAll() {
        return lessonRepo.findAll();
    }

    public boolean existsBySlug(String slug) {
        return lessonRepo.existsBySlug(slug);
    }

    public List<Lesson> listByStatus(Lesson.Status status) {
        return lessonRepo.findByStatus(status);
    }

    public List<Lesson> listByDifficulty(Lesson.Difficulty difficulty) {
        return lessonRepo.findByDifficulty(difficulty);
    }

    public List<Lesson> listByTag(Lesson.Tag tag) {
        return lessonRepo.findByTag(tag.name());
    }

    public List<Lesson> listByCreator(UUID creatorId) {
        return lessonRepo.findByCreator_Id(creatorId);
    }

    public List<Lesson> listPublished() {
        return lessonRepo.findPublished();
    }

    public List<Lesson> search(String keyword) {
        return lessonRepo.search(keyword);
    }

    @Transactional
    public Lesson create(Lesson lesson) {
        if (lesson.getSlug() != null && lessonRepo.existsBySlug(lesson.getSlug())) {
            throw new IllegalArgumentException("Slug already exists: " + lesson.getSlug());
        }
        lesson.setTotalQuestions(calculateTotalQuestions(lesson.getQuizJson()));
        return lessonRepo.save(lesson);
    }

    @Transactional
    public Lesson save(Lesson lesson) {
        // Nếu lesson đã tồn tại (có ID), khi update sẽ reset status về DRAFT
        if (lesson.getId() != null && lessonRepo.existsById(lesson.getId())) {
            // Giữ nguyên status nếu đang là DRAFT, còn nếu đang PENDING/APPROVED/REJECTED
            // thì về DRAFT
        }
        lesson.setTotalQuestions(calculateTotalQuestions(lesson.getQuizJson()));
        return lessonRepo.save(lesson);
    }

    @Transactional
    public void delete(UUID lessonId) {
        Lesson existing = getById(lessonId);
        lessonRepo.delete(existing);
    }

    @Transactional
    public Lesson moderateLesson(UUID moderatorId, UUID lessonId, Lesson.Status decision, String commentByMod) {
        if (decision != Lesson.Status.APPROVED && decision != Lesson.Status.REJECTED) {
            throw new IllegalArgumentException("Moderator can only approve or reject a lesson.");
        }

        Lesson lesson = getById(lessonId);

        if (lesson.getStatus() != Lesson.Status.PENDING) {
            throw new IllegalStateException(
                    "Only pending lessons can be moderated. Current status: " + lesson.getStatus());
        }
        // Bỏ check assignment ở đây vì logic mới là ai cũng có thể vào duyệt bài
        // PENDING

        if (decision == Lesson.Status.REJECTED && (commentByMod == null || commentByMod.isBlank())) {
            throw new IllegalArgumentException("Comment is required when rejecting a lesson.");
        }

        // Gán moderator cho bài học khi họ quyết định duyệt/từ chối
        Moderator moderator = new Moderator();
        moderator.setId(moderatorId);
        lesson.setModerator(moderator);

        LocalDateTime now = LocalDateTime.now();
        lesson.setStatus(decision);
        lesson.setCommentByMod(decision == Lesson.Status.APPROVED ? null : commentByMod);
        lesson.setUpdatedAt(now);
        if (decision == Lesson.Status.APPROVED) {
            lesson.setApprovedAt(now);
            if (lesson.getPublishedAt() == null) {
                lesson.setPublishedAt(now);
            }
        }
        return lessonRepo.save(lesson);
    }

    @Transactional
    public Lesson submitLesson(UUID lessonId) {
        Lesson lesson = getById(lessonId);
        if (lesson.getStatus() != Lesson.Status.DRAFT) {
            throw new IllegalStateException("Only draft lessons can be submitted.");
        }
        lesson.setStatus(Lesson.Status.PENDING);
        lesson.setUpdatedAt(LocalDateTime.now());
        return lessonRepo.save(lesson);
    }
}
