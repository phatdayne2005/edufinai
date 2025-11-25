package vn.uth.gamificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.gamificationservice.model.Challenge;
import vn.uth.gamificationservice.model.ChallengeScope;
import vn.uth.gamificationservice.model.UserChallengeProgress;
import vn.uth.gamificationservice.repository.UserChallengeProgressRepository;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Đảm nhiệm reset tiến trình challenge theo chu kỳ (daily / weekly / monthly).
 * - Không reset các challenge ALLTIME, ONEOFF hay SEASONAL (season xác định bởi start/end riêng).
 * - Chỉ reset khi challenge đang active và đang nằm trong khoảng thời gian hợp lệ.
 */
@Service
public class ChallengeResetService {

    private static final Logger log = LoggerFactory.getLogger(ChallengeResetService.class);
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final UserChallengeProgressRepository progressRepository;

    public ChallengeResetService(UserChallengeProgressRepository progressRepository) {
        this.progressRepository = progressRepository;
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void resetDailyChallenges() {
        resetProgressForScope(ChallengeScope.DAILY);
    }

    @Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void resetWeeklyChallenges() {
        resetProgressForScope(ChallengeScope.WEEKLY);
    }

    @Scheduled(cron = "0 0 0 1 * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void resetMonthlyChallenges() {
        resetProgressForScope(ChallengeScope.MONTHLY);
    }

    /**
     * Hàm hỗ trợ cho unit test / manual trigger.
     *
     * @return số bản ghi đã reset
     */
    @Transactional
    public int resetProgressForScope(ChallengeScope scope) {
        if (!EnumSet.of(ChallengeScope.DAILY, ChallengeScope.WEEKLY, ChallengeScope.MONTHLY).contains(scope)) {
            return 0;
        }

        ZonedDateTime now = ZonedDateTime.now(VIETNAM_ZONE);
        List<UserChallengeProgress> progresses = progressRepository.findByChallenge_Scope(scope);
        List<UserChallengeProgress> toUpdate = new ArrayList<>();
        List<UserChallengeProgress> toDelete = new ArrayList<>();

        for (UserChallengeProgress progress : progresses) {
            Challenge challenge = progress.getChallenge();
            if (challenge == null) {
                continue;
            }
            if (!challenge.isActive()) {
                continue;
            }
            if (now.isBefore(challenge.getStartAt()) || now.isAfter(challenge.getEndAt())) {
                continue;
            }

            // Lưu trạng thái trước khi reset
            boolean wasCompleted = Boolean.TRUE.equals(progress.getCompleted());
            
            // Reset tiến độ về 0
            progress.setCurrentProgress(0);
            progress.setCompleted(false);
            progress.setCompletedAt(null);
            progress.setProgressCountToday(0);
            progress.setLastProgressDate(null);
            
            // Nếu đã completed trước đó, giữ record để lưu lịch sử (startedAt giữ nguyên)
            if (wasCompleted) {
                progress.setUpdatedAt(now);
                toUpdate.add(progress);
            } 
            // Nếu chưa completed, xóa record để trạng thái là "chưa tham gia"
            // (vì sau khi reset, currentProgress = 0, nên không cần giữ record)
            else {
                toDelete.add(progress);
            }
        }

        if (!toDelete.isEmpty()) {
            progressRepository.deleteAll(toDelete);
            log.info("Deleted {} progress records with no progress for scope {}", toDelete.size(), scope);
        }

        if (!toUpdate.isEmpty()) {
            progressRepository.saveAll(toUpdate);
            log.info("Reset {} progress records for scope {}", toUpdate.size(), scope);
        }

        return toUpdate.size() + toDelete.size();
    }
}


