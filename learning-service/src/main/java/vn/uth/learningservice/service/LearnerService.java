package vn.uth.learningservice.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.learningservice.model.Learner;
import vn.uth.learningservice.model.Lesson;
import vn.uth.learningservice.repository.LearnerRepository;
import vn.uth.learningservice.repository.LessonRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearnerService {

    private final LearnerRepository learnerRepo;
    private final LessonRepository lessonRepo;

    public Learner getById(UUID id) {
        return learnerRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Learner not found: " + id));
    }

    @Transactional
    public Learner getOrCreate(UUID id) {
        return learnerRepo.findById(id).orElseGet(() -> {
            Learner newLearner = new Learner();
            newLearner.setId(id);
            return learnerRepo.save(newLearner);
        });
    }

    public List<Learner> listAll() {
        return learnerRepo.findAll();
    }

    public List<Learner> listByLevel(Learner.Level level) {
        return learnerRepo.findByLevel(level);
    }

    /**
     * Add correct answers and update EXP percentage
     * EXP% is calculated based on: (total correct answers / level up threshold) ×
     * 100%
     * Level up threshold = 80% of total questions in current difficulty
     */
    @Transactional
    public void addCorrectAnswers(UUID learnerId, int correctAnswersDelta) {
        if (correctAnswersDelta <= 0)
            return;

        Learner learner = learnerRepo.findById(learnerId)
                .orElseThrow(() -> new EntityNotFoundException("Learner not found: " + learnerId));

        // Update total correct answers
        learner.setTotalExp(learner.getTotalExp() + correctAnswersDelta);

        // Calculate EXP% and check for level up
        updateExpPercentAndLevel(learner);

        learnerRepo.save(learner);
    }

    /**
     * Calculate EXP percentage based on current level difficulty
     * and check if learner should level up
     */
    private void updateExpPercentAndLevel(Learner learner) {
        Lesson.Difficulty currentDifficulty = getDifficultyForLevel(learner.getLevel());

        // Get total questions for current difficulty (APPROVED lessons only)
        int totalQuestionsInDifficulty = lessonRepo.getTotalQuestionsByDifficulty(currentDifficulty);

        if (totalQuestionsInDifficulty == 0) {
            // No lessons in this difficulty yet, set EXP to 0%
            learner.setExpPercent(0);
            return;
        }

        // Level up threshold = 80% of total questions
        int levelUpThreshold = (int) Math.ceil(totalQuestionsInDifficulty * 0.8);

        // Calculate EXP percentage
        long totalCorrectAnswers = learner.getTotalExp();
        double expPercent = ((double) totalCorrectAnswers / levelUpThreshold) * 100.0;

        // Check for level up
        if (expPercent >= 100.0) {
            // Level up!
            if (learner.getLevel() == Learner.Level.BEGINNER) {
                learner.setLevel(Learner.Level.INTERMEDIATE);
            } else if (learner.getLevel() == Learner.Level.INTERMEDIATE) {
                learner.setLevel(Learner.Level.ADVANCED);
            }
            // If already ADVANCED, stay at ADVANCED

            // Reset EXP to 0% and reset totalExp counter for new level
            learner.setExpPercent(0);
            learner.setTotalExp(0L);

            // Recalculate for new level (in case they earned a lot of points)
            updateExpPercentAndLevel(learner);
        } else {
            // Update EXP percentage (capped at 100)
            learner.setExpPercent(Math.min(100, (int) Math.round(expPercent)));
        }
    }

    /**
     * Get the difficulty level that corresponds to a learner level
     */
    private Lesson.Difficulty getDifficultyForLevel(Learner.Level level) {
        return switch (level) {
            case BEGINNER -> Lesson.Difficulty.BASIC;
            case INTERMEDIATE -> Lesson.Difficulty.INTERMEDIATE;
            case ADVANCED -> Lesson.Difficulty.ADVANCED;
        };
    }

    /**
     * Legacy method for backward compatibility - converts delta to correct answers
     * 
     * @deprecated Use addCorrectAnswers instead
     */
    @Deprecated
    @Transactional
    public void addExp(UUID learnerId, long delta) {
        if (delta <= 0)
            return;

        // Convert old EXP points to correct answers (old logic: 1 correct = 10 exp)
        int correctAnswers = (int) (delta / 10);
        addCorrectAnswers(learnerId, correctAnswers);
    }
}
