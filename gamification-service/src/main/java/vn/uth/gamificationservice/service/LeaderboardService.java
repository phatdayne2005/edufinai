package vn.uth.gamificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import vn.uth.gamificationservice.dto.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;

@Service
public class LeaderboardService {
    private static final Logger log = LoggerFactory.getLogger(LeaderboardService.class);
    private final RedisTemplate<String, String> redisTemplate;
    private final UserService userService;

    private static final String LEADERBOARD_PREFIX = "leaderboard:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final WeekFields WEEK_FIELDS = WeekFields.of(Locale.getDefault());

    public LeaderboardService(RedisTemplate<String, String> redisTemplate, UserService userService) {
        this.redisTemplate = redisTemplate;
        this.userService = userService;
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

        int rank = 1;

        for (String memberStr : members) {
            UUID member = UUID.fromString(memberStr);
            Double score = redisTemplate.opsForZSet().score(key, memberStr);
            result.add(buildEntry(member, score != null ? score : 0.0, rank));
            rank++;
        }

        return new LeaderboardResponse(result, "SUCCESS");
    }

    /**
     * Lấy top của người dùng hiện tại
     */

    public ApiResponse<LeaderboardEntry> getCurrentUserTop(LeaderboardType type) {
        UserInfo userInfo = this.userService.getMyInfo();
        String key = getLeaderboardKey(type);
        String userIdStr = userInfo.getId().toString();

        Long myRankZeroBased = redisTemplate.opsForZSet().reverseRank(key, userIdStr);
        if (myRankZeroBased == null) {
            return ApiResponse.empty();
        }

        int myRank = myRankZeroBased.intValue() + 1;
        Double myScore = redisTemplate.opsForZSet().score(key, userIdStr);
        double safeScore = myScore != null ? myScore : 0.0;

        LeaderboardEntry myTopInfo = new LeaderboardEntry(
                userInfo.getId(),
                safeScore,
                myRank,
                buildFullName(userInfo.getFirstName(), userInfo.getLastName()),
                userInfo.getUsername()
        );
        ApiResponse<LeaderboardEntry> resp = new ApiResponse(200, myTopInfo, "SUCCESS");

        return resp;
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

    private LeaderboardEntry buildEntry(UUID userId, double score, int rank) {
        LeaderboardEntry entry = new LeaderboardEntry();
        entry.setUserId(userId);
        entry.setScore(score);
        entry.setTop(rank);

        try {
            UserInfo info = userService.getUserInfoById(userId);
            if (info != null) {
                entry.setUsername(info.getUsername());
                entry.setName(buildFullName(info.getFirstName(), info.getLastName()));
            }
        } catch (Exception ex) {
            log.warn("Could not enrich leaderboard entry for user {}", userId, ex);
        }

        return entry;
    }

    private String buildFullName(String firstName, String lastName) {
        if (!StringUtils.hasText(firstName) && !StringUtils.hasText(lastName)) {
            return null;
        }
        if (!StringUtils.hasText(firstName)) {
            return lastName;
        }
        if (!StringUtils.hasText(lastName)) {
            return firstName;
        }
        return firstName + " " + lastName;
    }
}
