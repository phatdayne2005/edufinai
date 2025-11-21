package vn.uth.edufinai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Value("${edufinai.http.maxInMemorySizeBytes:16777216}") // 16MB
    private int maxInMemorySize;

    @Value("${edufinai.http.connectTimeoutMs:3000}")
    private int connectTimeoutMs;

    @Value("${edufinai.http.responseTimeoutMs:10000}")
    private int responseTimeoutMs;

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // Đảm bảo serialize dates dạng ISO 8601 string, không phải timestamp
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Set timezone UTC
        mapper.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return mapper;
    }

    /**
     * Jackson2ObjectMapperBuilder để Spring WebFlux sử dụng đúng cấu hình
     */
    @Bean
    @Primary
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.modules(new JavaTimeModule());
        builder.featuresToDisable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        builder.timeZone(java.util.TimeZone.getTimeZone("UTC"));
        return builder;
    }

    private WebClient.Builder createBaseBuilder() {
        ConnectionProvider provider = ConnectionProvider.builder("edufinai-pool")
                .maxConnections(200)
                .pendingAcquireTimeout(Duration.ofSeconds(30))
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(responseTimeoutMs))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(responseTimeoutMs, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(responseTimeoutMs, TimeUnit.MILLISECONDS)));

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs((ClientCodecConfigurer configurer) ->
                        configurer.defaultCodecs().maxInMemorySize(maxInMemorySize))
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies);
    }

    @Bean
    @Primary
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return createBaseBuilder();
    }

    @Bean
    @Qualifier("plainWebClientBuilder")
    public WebClient.Builder plainWebClientBuilder() {
        return createBaseBuilder();
    }

    @Bean
    public TransactionTemplate transactionTemplate(@Autowired PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}

