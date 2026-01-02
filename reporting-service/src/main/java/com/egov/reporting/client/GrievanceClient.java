package com.egov.reporting.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import com.egov.reporting.dto.GrievanceDTO;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class GrievanceClient {

    private final WebClient.Builder webClientBuilder;

    public Flux<GrievanceDTO> getGrievances(String userId, String role, String status, String departmentId) {
        return webClientBuilder.build()
                .get()
                .uri(uriBuilder -> {
                    uriBuilder.scheme("http")
                              .host("grievance-service")
                              .path("/grievances");
                    
                    if (status != null) uriBuilder.queryParam("status", status);
                    if (departmentId != null) uriBuilder.queryParam("departmentId", departmentId);
                    
                    return uriBuilder.build();
                })
                .header("X-USER-ID", userId)
                .header("X-USER-ROLE", role)
                .retrieve()
                .bodyToFlux(GrievanceDTO.class);
    }

    public Flux<GrievanceDTO> getSlaBreaches(String userId, String role) {
        return webClientBuilder.build()
                .get()
                .uri("http://grievance-service/grievances/sla-breaches")
                .header("X-USER-ID", userId)
                .header("X-USER-ROLE", role)
                .retrieve()
                .bodyToFlux(GrievanceDTO.class);
    }
}