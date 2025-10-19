package vn.uth.gamificationservice.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import vn.uth.gamificationservice.dto.LeaderboardEntry;
import vn.uth.gamificationservice.dto.LeaderboardResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class LeaderboardService {
    public final RedisTemplate redisTemplate;

    private static final String LEADERBOARD_KEY =  "leaderboard:global";

    public LeaderboardService(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public LeaderboardResponse getTop(int topNumber) {
        Set<String> members = redisTemplate.opsForZSet().reverseRange(LEADERBOARD_KEY, 0, topNumber - 1);
        if (members == null || members.isEmpty())
            return new LeaderboardResponse(Collections.emptyList(), "SUCCESS");

        List<LeaderboardEntry> result = new ArrayList<>();

        for (String member : members) {
            Double score = redisTemplate.opsForZSet().score(LEADERBOARD_KEY, member);
            result.add(new LeaderboardEntry(member, score != null ? score : 0.0));
        }

        return new LeaderboardResponse(result, "SUCCESS");
    }
}
