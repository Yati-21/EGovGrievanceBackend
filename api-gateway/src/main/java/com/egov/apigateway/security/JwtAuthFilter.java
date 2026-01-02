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

	//public endpoints (no jwt required)
	private static final List<String> PUBLIC_PATHS = List.of("/auth/login","/auth/register", "/reference");


	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) 
	{
		String path = exchange.getRequest().getURI().getPath();
		String method = exchange.getRequest().getMethod().name();
		String internalCall = exchange.getRequest().getHeaders().getFirst("X-INTERNAL-CALL");

		// allow public endpoints
		if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
			return chain.filter(exchange);
		}

		// extract authorization header
		String authHeader = exchange.getRequest()
				.getHeaders()
				.getFirst(HttpHeaders.AUTHORIZATION);

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
			return exchange.getResponse().setComplete();
		}

		String token = authHeader.substring(7);

		try {
			// validate jwt token
			Claims claims = jwtUtil.validateAndGetClaims(token);

			String userId = claims.getSubject();
			String role = claims.get("role", String.class);

			// role-based route - authorization
			if (!hasAccess(path, role, method, internalCall)) {
				exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
				return exchange.getResponse().setComplete();
			}

			// forward headers to services
			ServerWebExchange mutatedExchange = exchange.mutate()
					.request(builder -> builder.header("X-USER-ID", userId).header("X-USER-ROLE", role)).build();
			return chain.filter(mutatedExchange);
		} 
		catch (Exception ex) {
			//ex.printStackTrace();
			exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
			return exchange.getResponse().setComplete();
		}
	}

	//role->route map  (role based security)
	private boolean hasAccess(String path, String role, String method,String internalCall) {

		// assign grievance
		if (path.matches(".*/grievances/.*/assign") && method.equals("PUT")) {
			return role.equals("ADMIN") || role.equals("SUPERVISOR");
		}

		// escalate grievance
		if (path.matches(".*/grievances/.*/escalate") && method.equals("PUT")) {
			return role.equals("CITIZEN");
		}

        //officer-only 
        if ((path.matches(".*/grievances/.*/in-review")|| path.matches(".*/grievances/.*/resolve")) && method.equals("PUT")) {
			return role.equals("OFFICER");
		}

        //close grievance
		if (path.matches(".*/grievances/.*/close") && method.equals("PUT")) {
            return role.equals("ADMIN")|| role.equals("SUPERVISOR")|| role.equals("OFFICER");
		}

        //reports
		if (path.startsWith("/reports") && method.equals("GET")) {
			return role.equals("ADMIN") || role.equals("SUPERVISOR") || role.equals("OFFICER");
		}

		if (path.startsWith("/users/supervisor/department") && method.equals("GET")) {
			return "true".equalsIgnoreCase(internalCall); // trust internal call
		}

		// update own profile
		if (path.matches("/users/[^/]+$") && method.equals("PUT")) {
            return role.equals("CITIZEN") || role.equals("OFFICER")|| role.equals("SUPERVISOR") || role.equals("ADMIN");
		}

		// admin-only operations
		if (path.matches("/users/.*/role/.*") && method.equals("PUT")
				|| path.matches("/users/.*/department/.*") && method.equals("PUT")
				|| path.startsWith("/users/role/") && method.equals("PUT")) {
			return role.equals("ADMIN");
		}

		// default -- authenticated user allowed
		return true;
	}

}
