package com.egov.notification.consumer;

import com.egov.notification.dto.UserResponse;
import com.egov.notification.event.GrievanceStatusChangedEvent;
import com.egov.notification.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrievanceStatusConsumerTest {

    @Mock
    private EmailService emailService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient.Builder webClientBuilder;

    @InjectMocks
    private GrievanceStatusConsumer consumer;

    private GrievanceStatusChangedEvent event;
    private UserResponse mockUser;

    @BeforeEach
    void setUp() {
        event = new GrievanceStatusChangedEvent();
        event.setGrievanceId("G123");
        event.setCitizenId("CITIZEN1");
        event.setOldStatus("OPEN");
        event.setNewStatus("IN_PROGRESS");
        event.setAssignedOfficerId("OFFICER1");

        mockUser = new UserResponse();
        mockUser.setEmail("test@test.com");
        mockUser.setName("Test User");
        mockUser.setRole("OFFICER");

        when(webClientBuilder.build().get().uri(anyString(), anyString())
                .retrieve().bodyToMono(UserResponse.class))
                .thenReturn(Mono.just(mockUser));
    }

    @Test
    void consume_NormalStatus_ShouldNotifyCitizenAndOfficer() {
        consumer.consume(event);
        verify(emailService, times(1)).sendCitizenMail(eq("test@test.com"), anyString(), eq("G123"), anyString(), eq("IN_PROGRESS"));
        verify(emailService, times(1)).sendOfficerMail(eq("test@test.com"), anyString(), anyString(), eq("G123"), eq("IN_PROGRESS"));
        verify(emailService, never()).sendSupervisorMail(any(), any(), any(), any());
    }

    @Test
    void consume_EscalatedStatus_ShouldNotifyCitizenAndSupervisor() {
        event.setNewStatus("ESCALATED");
        consumer.consume(event);
        verify(emailService, times(1)).sendCitizenMail(anyString(), anyString(), anyString(), anyString(), eq("ESCALATED"));
        verify(emailService, times(1)).sendSupervisorMail(eq("test@test.com"), anyString(), anyString(), eq("G123"));
        verify(emailService, never()).sendOfficerMail(any(), any(), any(), any(), any());
    }
}