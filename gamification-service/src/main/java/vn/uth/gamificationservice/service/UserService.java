package vn.uth.gamificationservice.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import vn.uth.gamificationservice.dto.ApiResponse;
import vn.uth.gamificationservice.dto.UserInfo;

import java.util.UUID;

@Service
public class UserService {
    public final RestTemplate restTemplate;

    private static final String AUTH_URL = "http://auth-service";
    private static final String INTERNAL_CLIENT_HEADER = "EduFinAI-Internal-Secret";

    public UserService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public UserInfo getMyInfo() {
        String url = AUTH_URL + "/identity/users/my-info";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Idempotency-Key", UUID.randomUUID().toString());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ApiResponse<UserInfo>> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            new ParameterizedTypeReference<ApiResponse<UserInfo>>() {}
                    );

            ApiResponse<UserInfo> body = response.getBody();

            if (body == null || body.getResult() == null) {
                throw new IllegalStateException("Empty response from auth-service");
            }

            // ⬇️ Trả luôn full object result
            return body.getResult();

        } catch (RestClientException ex) {
            throw new IllegalStateException("Error calling auth-service my-info", ex);
        }
    }

    public UserInfo getUserInfoById(UUID userId) {
        String url = AUTH_URL + "/identity/users/" + userId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Internal-Client", INTERNAL_CLIENT_HEADER);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ApiResponse<UserInfo>> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            new ParameterizedTypeReference<ApiResponse<UserInfo>>() {}
                    );

            ApiResponse<UserInfo> body = response.getBody();
            if (body == null || body.getResult() == null) {
                throw new IllegalStateException("Empty response from auth-service when fetching user by id");
            }
            return body.getResult();

        } catch (RestClientException ex) {
            throw new IllegalStateException("Error calling auth-service for user id " + userId, ex);
        }
    }
}
