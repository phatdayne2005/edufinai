package vn.uth.gamificationservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import vn.uth.gamificationservice.dto.ApiResponse;
import vn.uth.gamificationservice.dto.LeaderboardEntry;
import vn.uth.gamificationservice.dto.LeaderboardResponse;
import vn.uth.gamificationservice.dto.LeaderboardType;
import vn.uth.gamificationservice.dto.UserInfo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private UserService userService;

    @InjectMocks
    private LeaderboardService leaderboardService;

    @BeforeEach
    void setup() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    void shouldReturnOrderedDailyLeaderboardEntries() {
        LocalDate today = LocalDate.now();
        String key = "leaderboard:daily:" + today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UserInfo firstInfo = buildUserInfo(first, "firstUser", "Alice", "Nguyen");
        UserInfo secondInfo = buildUserInfo(second, "secondUser", "Bob", "Tran");
        Set<String> members = new LinkedHashSet<>();
        members.add(first.toString());
        members.add(second.toString());

        when(zSetOperations.reverseRange(key, 0, 1)).thenReturn(members);
        when(zSetOperations.score(key, first.toString())).thenReturn(150.0);
        when(zSetOperations.score(key, second.toString())).thenReturn(80.0);
        when(userService.getUserInfoById(first)).thenReturn(firstInfo);
        when(userService.getUserInfoById(second)).thenReturn(secondInfo);

        LeaderboardResponse response = leaderboardService.getTop(2, LeaderboardType.DAILY);

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getResult())
                .extracting(LeaderboardEntry::getUserId)
                .containsExactly(first, second);
        assertThat(response.getResult())
                .extracting(LeaderboardEntry::getScore)
                .containsExactly(150.0, 80.0);
        assertThat(response.getResult())
                .extracting(LeaderboardEntry::getTop)
                .containsExactly(1, 2);
        assertThat(response.getResult())
                .extracting(LeaderboardEntry::getUsername)
                .containsExactly("firstUser", "secondUser");
        assertThat(response.getResult())
                .extracting(LeaderboardEntry::getName)
                .containsExactly("Alice Nguyen", "Bob Tran");
    }

    @Test
    void shouldReturnCurrentUserStanding() {
        String key = "leaderboard:alltime";
        UUID userId = UUID.randomUUID();
        UserInfo userInfo = buildUserInfo(userId, "tester", "Henry", "Pham");
        when(userService.getMyInfo()).thenReturn(userInfo);
        when(zSetOperations.reverseRank(key, userId.toString())).thenReturn(4L);
        when(zSetOperations.score(key, userId.toString())).thenReturn(420.0);

        ApiResponse<LeaderboardEntry> response = leaderboardService.getCurrentUserTop(LeaderboardType.ALLTIME);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getResult().getUserId()).isEqualTo(userId);
        assertThat(response.getResult().getTop()).isEqualTo(5); // zero-based + 1
        assertThat(response.getResult().getScore()).isEqualTo(420.0);
        assertThat(response.getResult().getUsername()).isEqualTo("tester");
        assertThat(response.getResult().getName()).isEqualTo("Henry Pham");
    }

    private UserInfo buildUserInfo(UUID id, String username, String firstName, String lastName) {
        UserInfo info = new UserInfo();
        info.setId(id);
        info.setUsername(username);
        info.setFirstName(firstName);
        info.setLastName(lastName);
        return info;
    }
}


