package vn.uth.edufinai.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class GeminiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final int timeoutSeconds;

    public GeminiClient(@Qualifier("plainWebClientBuilder") WebClient.Builder builder,
                        ObjectMapper objectMapper,
                        @Value("${gemini.apiUrl:https://generativelanguage.googleapis.com/v1beta}") String apiUrl,
                        @Value("${gemini.apiKey:}") String apiKey,
                        @Value("${gemini.model}") String model,
                        @Value("${edufinai.gemini.timeoutSeconds:60}") int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.apiUrl = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
        
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(timeoutSeconds))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new io.netty.handler.timeout.ReadTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS))
                        .addHandlerLast(new io.netty.handler.timeout.WriteTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS)));
        
        this.webClient = builder
                .baseUrl(this.apiUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    public Mono<Result> callGemini(String prompt) {
        return callGemini(prompt, new Options());
    }

    public Mono<Result> callGemini(String prompt, Options options) {
        if (this.apiKey == null || this.apiKey.isEmpty()) {
            return Mono.just(Result.error("API_KEY_MISSING", "Gemini API key is not configured"));
        }
        
        String modelToUse = this.model.startsWith("models/") ? this.model.substring(7) : this.model;
        String path = "/models/" + modelToUse + ":generateContent";
        
        Map<String, Object> request = new HashMap<>();
        request.put("contents", new Object[] { 
            Map.of("parts", new Object[] { Map.of("text", Objects.requireNonNullElse(prompt, "")) }) 
        });
        if (options.temperature != null) {
            request.put("generationConfig", Map.of("temperature", options.temperature));
        }
        
        return webClient
                .post()
                .uri(uriBuilder -> uriBuilder.path(path).queryParam("key", this.apiKey).build())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), 
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(errorBody -> {
                                    String errorCode = "HTTP_" + response.statusCode().value();
                                    String errorMsg = "Unknown error";
                                    if (errorBody != null && !errorBody.isEmpty()) {
                                        try {
                                            JsonNode error = objectMapper.readTree(errorBody).path("error");
                                            if (!error.isMissingNode()) {
                                                errorMsg = error.path("message").asText("Unknown error");
                                                errorCode = error.path("code").asText(errorCode);
                                            } else {
                                                errorMsg = errorBody.length() > 500 ? errorBody.substring(0, 500) : errorBody;
                                            }
                                        } catch (Exception e) {
                                            errorMsg = errorBody.length() > 500 ? errorBody.substring(0, 500) : errorBody;
                                        }
                                    }
                                    return Mono.error(new RuntimeException(String.format("Gemini API error [%s]: %s", errorCode, errorMsg)));
                                }))
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .map(response -> response == null || response.isEmpty() 
                        ? Result.error("EMPTY_RESPONSE", "Gemini API returned null or empty response")
                        : toResult(response))
                .onErrorResume(ex -> Mono.just(Result.error("GEMINI_CALL_FAILED", ex.getMessage())));
    }

    private Result toResult(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                return Result.error(error.path("code").asText("UNKNOWN_ERROR"), 
                        error.path("message").asText("Unknown error")).withRawJson(rawJson);
            }
            
            String text = null;
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    text = parts.get(0).path("text").asText(null);
                }
            }
            
            Usage usage = new Usage();
            JsonNode usageNode = root.path("usageMetadata");
            if (!usageNode.isMissingNode()) {
                usage.promptTokens = usageNode.path("promptTokenCount").asInt(0);
                usage.candidatesTokens = usageNode.path("candidatesTokenCount").asInt(0);
                usage.totalTokens = usageNode.path("totalTokenCount").asInt(0);
            }
            
            Result r = new Result();
            r.ok = true;
            r.answerText = text != null ? text : "";
            r.rawJson = rawJson;
            r.usage = usage;
            r.model = this.model;
            r.timestamp = ZonedDateTime.now();
            
            if (r.answerText.isEmpty()) {
                r.ok = false;
                r.errorCode = "EMPTY_RESPONSE";
                r.errorMessage = "Gemini API returned empty response";
            }
            
            return r;
        } catch (Exception e) {
            return Result.error("PARSE_ERROR", e.getMessage()).withRawJson(rawJson);
        }
    }

    public static class Options {
        public Double temperature;
    }

    public static class Usage {
        public int promptTokens;
        public int candidatesTokens;
        public int totalTokens;
    }

    public static class Result {
        public boolean ok;
        public String answerText;
        public String rawJson;
        public String errorCode;
        public String errorMessage;
        public String model;
        public Usage usage;
        public ZonedDateTime timestamp;

        public static Result error(String code, String message) {
            Result r = new Result();
            r.ok = false;
            r.errorCode = code;
            r.errorMessage = message;
            r.timestamp = ZonedDateTime.now();
            return r;
        }

        public Result withRawJson(String rawJson) {
            this.rawJson = rawJson;
            return this;
        }
    }
}