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

    public void publishLessonCompleted(UUID userId, UUID lessonId, String enrollId, int rawScore) {
        ChallengeEventRequest request = new ChallengeEventRequest();
        request.setUserId(userId);
        request.setEventType("QUIZ");
        request.setAction("COMPLETE");
        request.setLessonId(lessonId);
        request.setEnrollId(enrollId);
        request.setScore(rawScore);
        request.setAmount(1);
        request.setOccurredAt(ZonedDateTime.now());
        challengeProgressService.processEvent(request);
    }
}

