package vn.uth.financeservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import vn.uth.financeservice.client.dto.AuthApiResponse;
import vn.uth.financeservice.client.dto.AuthUserResponse;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthServiceClient {

    private static final ParameterizedTypeReference<AuthApiResponse<AuthUserResponse>> USER_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;

    @Value("${services.auth.base-url:http://auth-service}")
    private String authServiceBaseUrl;

    public AuthUserResponse getCurrentUser() {
        String url = authServiceBaseUrl + "/identity/users/my-info";
        try {
            ResponseEntity<AuthApiResponse<AuthUserResponse>> responseEntity =
                    restTemplate.exchange(url, HttpMethod.GET, null, USER_RESPONSE_TYPE);

            AuthApiResponse<AuthUserResponse> body = responseEntity.getBody();
            if (body == null || body.getResult() == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không thể lấy thông tin người dùng");
            }
            return body.getResult();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to call Auth Service for /users/my-info", ex);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không thể xác thực người dùng");
        }
    }

    public UUID getCurrentUserId() {
        AuthUserResponse user = getCurrentUser();
        if (user == null || !StringUtils.hasText(user.getId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Thiếu thông tin userId");
        }
        try {
            return UUID.fromString(user.getId());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Định dạng userId không hợp lệ");
        }
    }
}

