package vn.uth.gamificationservice.service;

import org.springframework.stereotype.Service;
import vn.uth.gamificationservice.dto.ChallengeEventRequest;

import java.time.ZonedDateTime;
import java.util.UUID;

@Service
public class ChallengeEventPublisher {

    private final ChallengeProgressService challengeProgressService;

    public ChallengeEventPublisher(ChallengeProgressService challengeProgressService) {
        this.challengeProgressService = challengeProgressService;
    }

    public void publishLessonCompleted(UUID userId,
                                       UUID lessonId,
                                       String enrollId,
                                       int rawScore,
                                       int accuracyPercent,
                                       int totalQuestions,
                                       int correctAnswers) {
        ChallengeEventRequest request = new ChallengeEventRequest();
        request.setUserId(userId);
        request.setEventType("QUIZ");
        request.setAction("COMPLETE");
        request.setLessonId(lessonId);
        request.setEnrollId(enrollId);
        request.setScore(rawScore);
        request.setAccuracyPercent(accuracyPercent);
        request.setTotalQuestions(totalQuestions);
        request.setCorrectAnswers(correctAnswers);
        request.setAmount(1);
        request.setOccurredAt(ZonedDateTime.now());
        challengeProgressService.processEvent(request);
    }
}

