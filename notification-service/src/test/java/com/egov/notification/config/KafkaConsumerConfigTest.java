package com.egov.notification.config;

import com.egov.notification.event.GrievanceStatusChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class KafkaConsumerConfigTest {

	private KafkaConsumerConfig config;

	@BeforeEach
	void setUp() {
		config = new KafkaConsumerConfig();
		ReflectionTestUtils.setField(config, "bootstrapServers", "localhost:9092");
	}

	@Test
	void consumerFactory_created() {
		ConsumerFactory<String, GrievanceStatusChangedEvent> factory = config.consumerFactory();
		assertNotNull(factory);
	}
}
