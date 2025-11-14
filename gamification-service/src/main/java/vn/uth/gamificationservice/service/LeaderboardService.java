package vn.uth.gamificationservice.service;

import jakarta.ws.rs.core.MediaType;
import org.apache.hc.core5.http.HttpEntity;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import vn.uth.gamificationservice.dto.*;

import java.net.http.HttpHeaders;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;

@Service
public class LeaderboardService {
    private final RedisTemplate redisTemplate;
    private final UserService userService;

    private static final String LEADERBOARD_PREFIX = "leaderboard:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final WeekFields WEEK_FIELDS = WeekFields.of(Locale.getDefault());

    public LeaderboardService(RedisTemplate redisTemplate, UserService userService) {
        this.redisTemplate = redisTemplate;
        this.userService = userService;
    }

    /**
     * Lấy top leaderboard theo type
     */
    public LeaderboardResponse getTop(int topNumber, LeaderboardType type) {
        String key = getLeaderboardKey(type);
        Set<UUID> members = redisTemplate.opsForZSet().reverseRange(key, 0, topNumber - 1);
        if (members == null || members.isEmpty())
            return new LeaderboardResponse(Collections.emptyList(), "SUCCESS");

        List<LeaderboardEntry> result = new ArrayList<>();

        int rank = 1;

        for (UUID member : members) {
            Double score = redisTemplate.opsForZSet().score(key, member);
            result.add(new LeaderboardEntry(member, score != null ? score : 0.0, rank));
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


        Long myRankZeroBased = redisTemplate.opsForZSet().reverseRank(key, userInfo.getId());
        if (myRankZeroBased == null) {
            return ApiResponse.empty();
        }

        int myRank = myRankZeroBased.intValue() + 1;
        Double myScore = redisTemplate.opsForZSet().score(key, userInfo.getId());
        double safeScore = myScore != null ? myScore : 0.0;

        LeaderboardEntry myTopInfo = new LeaderboardEntry(userInfo.getId(), safeScore, myRank);
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
}
