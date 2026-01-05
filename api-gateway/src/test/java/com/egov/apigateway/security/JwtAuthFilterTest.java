package com.egov.apigateway.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

	@Mock
	private JwtUtil jwtUtil;

	@Mock
	private GatewayFilterChain chain;

	private JwtAuthFilter filter;

	@BeforeEach
	void setup() {
		filter = new JwtAuthFilter(jwtUtil);
	}

	@Test
	void publicEndpoint_allowedWithoutToken() {
		when(chain.filter(any())).thenReturn(Mono.empty());
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/auth/login").build());
		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
		verify(chain).filter(any());
	}

	@Test
	void missingAuthorizationHeader_returns401() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/grievances").build());
		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
		assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
		verifyNoInteractions(chain);
	}

	@Test
	void invalidToken_returns401() {
		when(jwtUtil.validateAndGetClaims(anyString())).thenThrow(new RuntimeException("bad token"));
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/grievances").header(HttpHeaders.AUTHORIZATION, "Bearer badtoken").build());
		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
		assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
		verifyNoInteractions(chain);
	}

	@Test
	void forbiddenRole_returns403() {
		Claims claims = mock(Claims.class);
		when(claims.getSubject()).thenReturn("USER1");
		when(claims.get("role", String.class)).thenReturn("CITIZEN");
		when(jwtUtil.validateAndGetClaims(anyString())).thenReturn(claims);
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.put("/grievances/1/assign")
				.header(HttpHeaders.AUTHORIZATION, "Bearer token").build());
		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
		assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
		verifyNoInteractions(chain);
	}

	@Test
	void validToken_forwardHeaders_success() {
		when(chain.filter(any())).thenReturn(Mono.empty());
		Claims claims = mock(Claims.class);
		when(claims.getSubject()).thenReturn("USER99");
		when(claims.get("role", String.class)).thenReturn("ADMIN");
		when(jwtUtil.validateAndGetClaims(anyString())).thenReturn(claims);
		MockServerWebExchange exchange = MockServerWebExchange
				.from(MockServerHttpRequest.get("/reports").header(HttpHeaders.AUTHORIZATION, "Bearer token").build());
		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
		verify(chain).filter(any());
	}

	@Test
	void hasAccess_GrievanceAssign_AllowedForAdmin() {
		when(chain.filter(any())).thenReturn(Mono.empty());
		mockJwtAndFilter("/grievances/123/assign", "PUT", "ADMIN");
		verify(chain).filter(any());
	}

	@Test
	void hasAccess_Escalate_AllowedForCitizen() {
		when(chain.filter(any())).thenReturn(Mono.empty());
		mockJwtAndFilter("/grievances/123/escalate", "PUT", "CITIZEN");
		verify(chain).filter(any());
	}

	@Test
	void hasAccess_InReview_AllowedForOfficer() {
		when(chain.filter(any())).thenReturn(Mono.empty());
		mockJwtAndFilter("/grievances/123/in-review", "PUT", "OFFICER");
		verify(chain).filter(any());
	}

	@Test
	void hasAccess_Close_ForbiddenForCitizen() {
		mockJwtAndFilter("/grievances/123/close", "PUT", "CITIZEN");
		assertEquals(HttpStatus.FORBIDDEN, getStatusCode());
	}

	@Test
	void hasAccess_UserReports_AllowedForCitizen() {
		when(chain.filter(any())).thenReturn(Mono.empty());
		mockJwtAndFilter("/reports/user/summary", "GET", "CITIZEN");
		verify(chain).filter(any());
	}

	@Test
	void hasAccess_GeneralReports_ForbiddenForCitizen() {
		mockJwtAndFilter("/reports/all-dept", "GET", "CITIZEN");
		assertEquals(HttpStatus.FORBIDDEN, getStatusCode());
	}

	@Test
	void hasAccess_InternalCall_SupervisorPath() {
		when(chain.filter(any())).thenReturn(Mono.empty());
		MockServerWebExchange exchange = MockServerWebExchange
				.from(MockServerHttpRequest.get("/users/supervisor/department/D1")
						.header(HttpHeaders.AUTHORIZATION, "Bearer token").header("X-INTERNAL-CALL", "true").build());

		setupMockClaims("USER1", "CITIZEN");
		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
		verify(chain).filter(any());
	}

	@Test
	void hasAccess_GetUserProfile_OwnProfileAllowed() {
		when(chain.filter(any())).thenReturn(Mono.empty());
		mockJwtAndFilter("/users/USER1", "GET", "CITIZEN", "USER1");
		verify(chain).filter(any());
	}

	@Test
	void hasAccess_AdminOnly_RoleUpdate() {
		mockJwtAndFilter("/users/123/role/OFFICER", "PUT", "OFFICER");
		assertEquals(HttpStatus.FORBIDDEN, getStatusCode());
	}

	private void mockJwtAndFilter(String path, String method, String role) {
		mockJwtAndFilter(path, method, role, "DEFAULT_ID");
	}

	private void mockJwtAndFilter(String path, String method, String role, String userId) {
		MockServerWebExchange exchange = MockServerWebExchange
				.from(MockServerHttpRequest.method(org.springframework.http.HttpMethod.valueOf(method), path)
						.header(HttpHeaders.AUTHORIZATION, "Bearer token").build());

		setupMockClaims(userId, role);
		this.currentExchange = exchange;
		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
	}

	private void setupMockClaims(String userId, String role) {
		Claims claims = mock(Claims.class);
		when(claims.getSubject()).thenReturn(userId);
		when(claims.get("role", String.class)).thenReturn(role);
		when(jwtUtil.validateAndGetClaims(anyString())).thenReturn(claims);
	}

	private MockServerWebExchange currentExchange;

	private HttpStatus getStatusCode() {
		return (HttpStatus) currentExchange.getResponse().getStatusCode();
	}

	@Test
	void hasAccess_UpdateProfile_Success() {
		when(chain.filter(any())).thenReturn(Mono.empty());
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.put("/users/user123").header(HttpHeaders.AUTHORIZATION, "Bearer token").build());

		Claims claims = mock(Claims.class);
		when(claims.getSubject()).thenReturn("user123");
		when(claims.get("role", String.class)).thenReturn("OFFICER");
		when(jwtUtil.validateAndGetClaims(anyString())).thenReturn(claims);

		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
		verify(chain).filter(any());
	}

	@Test
	void hasAccess_AdminOperations_DeptUpdate_Success() {
		when(chain.filter(any())).thenReturn(Mono.empty());
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
				.put("/users/user123/department/DEPT1").header(HttpHeaders.AUTHORIZATION, "Bearer token").build());

		Claims claims = mock(Claims.class);
		when(claims.getSubject()).thenReturn("admin-id");
		when(claims.get("role", String.class)).thenReturn("ADMIN");
		when(jwtUtil.validateAndGetClaims(anyString())).thenReturn(claims);

		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
	}

	@Test
	void hasAccess_AdminOperations_RoleUpdate_Forbidden() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.put("/users/role/assign")
				.header(HttpHeaders.AUTHORIZATION, "Bearer token").build());
		Claims claims = mock(Claims.class);
		when(claims.getSubject()).thenReturn("user1");
		when(claims.get("role", String.class)).thenReturn("OFFICER");
		when(jwtUtil.validateAndGetClaims(anyString())).thenReturn(claims);

		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
		assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
	}

	@Test
	void hasAccess_DefaultAllowed() {
		when(chain.filter(any())).thenReturn(Mono.empty());
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/misc/info").header(HttpHeaders.AUTHORIZATION, "Bearer token").build());

		Claims claims = mock(Claims.class);
		when(claims.getSubject()).thenReturn("user1");
		when(claims.get("role", String.class)).thenReturn("CITIZEN");
		when(jwtUtil.validateAndGetClaims(anyString())).thenReturn(claims);
		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
		verify(chain).filter(any());
	}
}