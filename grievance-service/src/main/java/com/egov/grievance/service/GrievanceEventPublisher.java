package com.egov.grievance.service;

import com.egov.grievance.event.GrievanceStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrievanceEventPublisher {

    private static final String TOPIC = "grievance-status-changed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishStatusChange(GrievanceStatusChangedEvent event) {
        kafkaTemplate.send(TOPIC, event);
        log.info("Published grievance status change event: {}", event);
    }
}
