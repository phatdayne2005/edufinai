package vn.uth.learningservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.uth.learningservice.model.Lesson;
import vn.uth.learningservice.repository.LessonRepository;

import java.util.*;

@Service
public class LessonService {

    @Autowired
    private LessonRepository lessonRepo;

    public List<Lesson> getAllLessons() {
        return lessonRepo.findAll();
    }

    public Lesson getLessonById(UUID lessonId) {
        return lessonRepo.findById(lessonId).orElse(null);
    }

    public void addLesson(Lesson lesson) {
        lessonRepo.save(lesson);
    }

    public void updateLesson(Lesson lesson) {
        lessonRepo.save(lesson);
    }

    public void deleteLesson(UUID lessonId) {
        lessonRepo.deleteById(lessonId);
    }
}
