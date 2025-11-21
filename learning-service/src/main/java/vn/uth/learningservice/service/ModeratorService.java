package vn.uth.learningservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.uth.learningservice.model.*;
import vn.uth.learningservice.repository.*;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ModeratorService {

    private final ModeratorRepository moderatorRepo;
    private final LessonRepository lessonRepo;

    public Moderator getById(UUID id) {
        return moderatorRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Moderator not found: " + id));
    }

    // Dùng LessonRepository để đếm số bài PENDING đã gán cho moderator
    public long countPendingAssigned(UUID moderatorId) {
        return lessonRepo.countByModerator_IdAndStatus(moderatorId, Lesson.Status.PENDING);
    }

    // Lấy danh sách tất cả bài PENDING (không phân biệt moderator)
    // Tham số moderatorId giữ lại để tuân thủ API signature nhưng không dùng để lọc
    public List<Lesson> listPending(UUID moderatorId) {
        return lessonRepo.findByStatus(Lesson.Status.PENDING);
    }

    public List<Moderator> listAll() {
        return moderatorRepo.findAll();
    }
}
