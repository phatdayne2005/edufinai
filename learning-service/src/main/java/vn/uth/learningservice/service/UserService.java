package vn.uth.learningservice.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import vn.uth.learningservice.dto.ApiResponse;
import vn.uth.learningservice.dto.UserInfo;

import java.util.UUID;

@Service
public class UserService {
    public final RestTemplate restTemplate;

    private static final String AUTH_URL = "http://auth-service";

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
            ResponseEntity<ApiResponse<UserInfo>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<UserInfo>>() {
                    });

            ApiResponse<UserInfo> body = response.getBody();

            if (body == null || body.getResult() == null) {
                throw new IllegalStateException("Empty response from auth-service");
            }

            return body.getResult();

        } catch (RestClientException ex) {
            throw new IllegalStateException("Error calling auth-service my-info", ex);
        }
    }

    public UserInfo getUserById(UUID id) {
        String url = AUTH_URL + "/identity/users/" + id;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Idempotency-Key", UUID.randomUUID().toString());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ApiResponse<UserInfo>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<UserInfo>>() {
                    });

            ApiResponse<UserInfo> body = response.getBody();

            if (body == null || body.getResult() == null) {
                // Có thể user không tồn tại hoặc lỗi khác, trả về null hoặc throw
                return null;
            }

            return body.getResult();

        } catch (RestClientException ex) {
            // Log error but don't crash flow if user info fetch fails
            System.err.println("Error calling auth-service get-user-by-id: " + ex.getMessage());
            return null;
        }
    }
}
