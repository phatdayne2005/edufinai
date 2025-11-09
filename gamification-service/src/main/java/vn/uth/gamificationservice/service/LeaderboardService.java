package vn.uth.gamificationservice.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import vn.uth.gamificationservice.dto.LeaderboardEntry;
import vn.uth.gamificationservice.dto.LeaderboardResponse;
import vn.uth.gamificationservice.dto.LeaderboardType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class LeaderboardService {
    public final RedisTemplate redisTemplate;

    private static final String LEADERBOARD_PREFIX = "leaderboard:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final WeekFields WEEK_FIELDS = WeekFields.of(Locale.getDefault());

    public LeaderboardService(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Lấy top leaderboard theo type
     */
    public LeaderboardResponse getTop(int topNumber, LeaderboardType type) {
        String key = getLeaderboardKey(type);
        Set<String> members = redisTemplate.opsForZSet().reverseRange(key, 0, topNumber - 1);
        if (members == null || members.isEmpty())
            return new LeaderboardResponse(Collections.emptyList(), "SUCCESS");

        List<LeaderboardEntry> result = new ArrayList<>();

        for (String member : members) {
            Double score = redisTemplate.opsForZSet().score(key, member);
            result.add(new LeaderboardEntry(member, score != null ? score : 0.0));
        }

        return new LeaderboardResponse(result, "SUCCESS");
    }

    /**
     * Tạo key Redis dựa trên type của leaderboard
     */
    private String getLeaderboardKey(LeaderboardType type) {
        LocalDate now = LocalDate.now();

        switch (type) {
            case DAILY:
                return LEADERBOARD_PREFIX + "daily:" + now.format(DATE_FORMATTER);
            case WEEKLY:
                int year = now.getYear();
                int week = now.get(WEEK_FIELDS.weekOfWeekBasedYear());
                return LEADERBOARD_PREFIX + "weekly:" + year + "-W" + String.format("%02d", week);
            case MONTHLY:
                return LEADERBOARD_PREFIX + "monthly:" + now.format(MONTH_FORMATTER);
            case ALLTIME:
                return LEADERBOARD_PREFIX + "alltime";
            default:
                return LEADERBOARD_PREFIX + "alltime";
        }
    }

    /**
     * Lấy key cho leaderboard theo type (dùng trong RewardService)
     */
    public String getLeaderboardKeyForType(LeaderboardType type) {
        return getLeaderboardKey(type);
    }
}
