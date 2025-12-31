package com.egov.apigateway.security;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;

import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthFilter implements GlobalFilter {

	private final JwtUtil jwtUtil;

	public JwtAuthFilter(JwtUtil jwtUtil) {
		this.jwtUtil = jwtUtil;
	}

	private static final List<String> PUBLIC_PATHS = List.of("/auth/login", "/auth/register", "/reference");

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

		String path = exchange.getRequest().getURI().getPath();

		// allow public endpoints
		if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
			return chain.filter(exchange);
		}

		// extract authorization header
		String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
			return exchange.getResponse().setComplete();
		}

		String token = authHeader.substring(7);

		try {
			// validate token
			Claims claims = jwtUtil.validateAndGetClaims(token);

			String userId = claims.getSubject();
			String role = claims.get("role", String.class);

			// Forward headers to services
			ServerWebExchange mutatedExchange = exchange.mutate()
					.request(builder -> builder.header("X-USER-ID", userId).header("X-USER-ROLE", role)).build();

			return chain.filter(mutatedExchange);

		} 
		catch (Exception ex) {
			exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
			return exchange.getResponse().setComplete();
		}
	}
}
