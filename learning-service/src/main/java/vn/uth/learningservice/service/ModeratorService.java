package vn.uth.learningservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    public Moderator getOrCreate(UUID id) {
        return moderatorRepo.findById(id).orElseGet(() -> {
            Moderator newModerator = new Moderator();
            newModerator.setId(id);
            return moderatorRepo.save(newModerator);
        });
    }

    // Dùng LessonRepository để đếm số bài PENDING đã gán cho moderator
    public long countPendingAssigned(UUID moderatorId) {
        return lessonRepo.countByModerator_IdAndStatus(moderatorId, Lesson.Status.PENDING);
    }

    public List<Moderator> listAll() {
        return moderatorRepo.findAll();
    }
}
