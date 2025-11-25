package vn.uth.gamificationservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import vn.uth.gamificationservice.service.UserService;

@SpringBootTest
@ActiveProfiles("test")
class GamificationServiceApplicationTests {

    @MockBean(name = "redisTemplate")
    private RedisTemplate<String, String> redisTemplate;

    @MockBean(name = "stringRedisTemplate")
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private UserService userService;

    @Test
    void contextLoads() {
    }

}
