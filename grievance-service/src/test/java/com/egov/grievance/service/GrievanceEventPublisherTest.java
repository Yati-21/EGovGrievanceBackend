package com.egov.grievance.service;

import static org.mockito.Mockito.verify;

import com.egov.grievance.event.GrievanceStatusChangedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class GrievanceEventPublisherTest {

	@Mock
	private KafkaTemplate<String, Object> kafkaTemplate;

	@InjectMocks
	private GrievanceEventPublisher publisher;

	@Test
	void publishStatusChange_sendsEventToKafka() {
		GrievanceStatusChangedEvent event = new GrievanceStatusChangedEvent("G1", "C1", "D1", "O1", "SUBMITTED",
				"ASSIGNED", "ADMIN", java.time.Instant.now());
		publisher.publishStatusChange(event);
		verify(kafkaTemplate).send("grievance-status-changed", event);
	}
}
