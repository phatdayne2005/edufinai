package vn.uth.gamificationservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.gamificationservice.model.LessonAttemptHistory;
import vn.uth.gamificationservice.model.UserLessonScore;
import vn.uth.gamificationservice.repository.LessonAttemptHistoryRepository;
import vn.uth.gamificationservice.repository.UserLessonScoreRepository;

import java.util.UUID;

@Service
public class LessonScoreService {

    public record LessonAttemptResult(boolean duplicate, int deltaScore, int rawScore, int previousBest) {
        public boolean hasImproved() {
            return deltaScore > 0;
        }
    }

    private final UserLessonScoreRepository userLessonScoreRepository;
    private final LessonAttemptHistoryRepository lessonAttemptHistoryRepository;

    public LessonScoreService(UserLessonScoreRepository userLessonScoreRepository,
                              LessonAttemptHistoryRepository lessonAttemptHistoryRepository) {
        this.userLessonScoreRepository = userLessonScoreRepository;
        this.lessonAttemptHistoryRepository = lessonAttemptHistoryRepository;
    }

    @Transactional
    public LessonAttemptResult processAttempt(UUID userId, UUID lessonId, String enrollId, int rawScore) {
        if (enrollId == null || enrollId.isBlank()) {
            throw new IllegalArgumentException("Enroll ID is required for lesson attempt");
        }
        if (lessonId == null) {
            throw new IllegalArgumentException("Lesson ID is required for lesson attempt");
        }

        UserLessonScore lessonScore = userLessonScoreRepository
                .findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> createBaseline(userId, lessonId));

        boolean existed = lessonAttemptHistoryRepository.existsByEnrollId(enrollId);
        if (existed) {
            int previousBest = lessonScore.getBestScore() != null ? lessonScore.getBestScore() : 0;
            return new LessonAttemptResult(true, 0, rawScore, previousBest);
        }

        int previousBest = lessonScore.getBestScore() != null ? lessonScore.getBestScore() : 0;
        int delta = Math.max(rawScore - previousBest, 0);

        lessonScore.setLastScore(rawScore);
        lessonScore.setLastEnrollId(enrollId);
        int attempts = lessonScore.getAttemptCount() != null ? lessonScore.getAttemptCount() : 0;
        lessonScore.setAttemptCount(attempts + 1);
        if (delta > 0 || lessonScore.getBestScore() == null) {
            lessonScore.setBestScore(Math.max(previousBest, rawScore));
        }
        userLessonScoreRepository.save(lessonScore);

        LessonAttemptHistory history = new LessonAttemptHistory();
        history.setUserId(userId);
        history.setLessonId(lessonId);
        history.setEnrollId(enrollId);
        history.setRawScore(rawScore);
        history.setDeltaScore(delta);
        lessonAttemptHistoryRepository.save(history);

        return new LessonAttemptResult(false, delta, rawScore, previousBest);
    }

    private UserLessonScore createBaseline(UUID userId, UUID lessonId) {
        UserLessonScore score = new UserLessonScore();
        score.setUserId(userId);
        score.setLessonId(lessonId);
        score.setBestScore(0);
        score.setLastScore(0);
        score.setAttemptCount(0);
        return score;
    }
}

