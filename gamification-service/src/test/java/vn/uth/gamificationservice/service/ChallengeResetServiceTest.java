package vn.uth.gamificationservice.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import vn.uth.gamificationservice.model.Challenge;
import vn.uth.gamificationservice.model.ChallengeScope;
import vn.uth.gamificationservice.model.ChallengeType;
import vn.uth.gamificationservice.model.UserChallengeProgress;
import vn.uth.gamificationservice.repository.ChallengeRepository;
import vn.uth.gamificationservice.repository.UserChallengeProgressRepository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(ChallengeResetService.class)
class ChallengeResetServiceTest {

    @Autowired
    private ChallengeResetService challengeResetService;

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private UserChallengeProgressRepository progressRepository;

    @Test
    void resetDailyChallengesShouldOnlyAffectActiveDailyOnes() {
        Challenge daily = saveChallenge(ChallengeScope.DAILY, true, ZonedDateTime.now().minusDays(1), ZonedDateTime.now().plusDays(3));
        Challenge weekly = saveChallenge(ChallengeScope.WEEKLY, true, ZonedDateTime.now().minusDays(1), ZonedDateTime.now().plusDays(7));

        UserChallengeProgress dailyProgress = saveProgress(daily, 2, true);
        UserChallengeProgress weeklyProgress = saveProgress(weekly, 5, false);

        int resetCount = challengeResetService.resetProgressForScope(ChallengeScope.DAILY);

        assertThat(resetCount).isEqualTo(1);

        UserChallengeProgress refreshedDaily = progressRepository.findById(dailyProgress.getId()).orElseThrow();
        assertThat(refreshedDaily.getCurrentProgress()).isZero();
        assertThat(refreshedDaily.getCompleted()).isFalse();
        assertThat(refreshedDaily.getCompletedAt()).isNull();
        assertThat(refreshedDaily.getProgressCountToday()).isZero();
        assertThat(refreshedDaily.getLastProgressDate()).isNull();

        UserChallengeProgress refreshedWeekly = progressRepository.findById(weeklyProgress.getId()).orElseThrow();
        assertThat(refreshedWeekly.getCurrentProgress()).isEqualTo(5);
        assertThat(refreshedWeekly.getCompleted()).isFalse();
    }

    @Test
    void resetSkipsInactiveOrExpiredChallenges() {
        Challenge inactiveDaily = saveChallenge(ChallengeScope.DAILY, false, ZonedDateTime.now().minusDays(1), ZonedDateTime.now().plusDays(2));
        Challenge expiredDaily = saveChallenge(ChallengeScope.DAILY, true, ZonedDateTime.now().minusDays(5), ZonedDateTime.now().minusDays(1));
        UserChallengeProgress inactiveProgress = saveProgress(inactiveDaily, 3, true);
        UserChallengeProgress expiredProgress = saveProgress(expiredDaily, 4, true);

        int resetCount = challengeResetService.resetProgressForScope(ChallengeScope.DAILY);

        assertThat(resetCount).isZero();

        UserChallengeProgress refreshedInactive = progressRepository.findById(inactiveProgress.getId()).orElseThrow();
        assertThat(refreshedInactive.getCurrentProgress()).isEqualTo(3);
        assertThat(refreshedInactive.getCompleted()).isTrue();

        UserChallengeProgress refreshedExpired = progressRepository.findById(expiredProgress.getId()).orElseThrow();
        assertThat(refreshedExpired.getCurrentProgress()).isEqualTo(4);
        assertThat(refreshedExpired.getCompleted()).isTrue();
    }

    private Challenge saveChallenge(ChallengeScope scope, boolean active, ZonedDateTime start, ZonedDateTime end) {
        Challenge challenge = new Challenge();
        challenge.setTitle(scope + " test");
        challenge.setDescription("Reset test");
        challenge.setType(ChallengeType.QUIZ);
        challenge.setScope(scope);
        challenge.setTargetValue(5);
        challenge.setStartAt(start);
        challenge.setEndAt(end);
        challenge.setActive(active);
        challenge.setRule("{\"eventType\":\"QUIZ\",\"action\":\"COMPLETE\",\"count\":5}");
        challenge.setRewardScore(50);
        challenge.setRewardBadgeCode("RESET_" + scope);
        return challengeRepository.save(challenge);
    }

    private UserChallengeProgress saveProgress(Challenge challenge, int progressValue, boolean completed) {
        UserChallengeProgress progress = new UserChallengeProgress();
        progress.setUserId(UUID.randomUUID());
        progress.setChallenge(challenge);
        progress.setCurrentProgress(progressValue);
        progress.setTargetProgress(challenge.getTargetValue());
        progress.setCompleted(completed);
        progress.setCompletedAt(completed ? ZonedDateTime.now() : null);
        progress.setProgressCountToday(progressValue);
        progress.setLastProgressDate(LocalDate.now());
        progress.setStartedAt(ZonedDateTime.now().minusDays(2));
        progress.setUpdatedAt(ZonedDateTime.now());
        return progressRepository.save(progress);
    }
}


