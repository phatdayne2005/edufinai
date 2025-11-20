package vn.uth.firebasenotification.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import vn.uth.firebasenotification.dto.ApiResponse;
import vn.uth.firebasenotification.dto.UserInfo;

import java.util.UUID;

@Service
public class UserService {

    private static final String AUTH_BASE_URL = "http://auth-service";

    private final RestTemplate restTemplate;

    public UserService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public UserInfo getMyInfo() {
        String url = AUTH_BASE_URL + "/identity/users/my-info";

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
}
