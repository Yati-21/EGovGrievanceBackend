package com.egov.reporting.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class UserClient {

    private final WebClient client;

    public UserClient(WebClient.Builder builder) {
        this.client = builder.baseUrl("http://user-service").build();
    }

    public Mono<Boolean> validateUserExists(String userId) {
        return client.get()
                .uri("/users/{id}", userId)
                .retrieve()
                .toBodilessEntity()
                .map(response -> response.getStatusCode().is2xxSuccessful())
                // if 4xx/5xx, return false
                .onErrorReturn(false);
    }
}