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
	
	private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_SUPERVISOR = "SUPERVISOR";
    private static final String ROLE_OFFICER = "OFFICER";
    private static final String ROLE_CITIZEN = "CITIZEN";
    private static final String METHOD_PUT = "PUT";
    private static final String METHOD_GET = "GET";

	private final JwtUtil jwtUtil;

	public JwtAuthFilter(JwtUtil jwtUtil) {
		this.jwtUtil = jwtUtil;
	}

	//public endpoints (no jwt required)
	private static final List<String> PUBLIC_PATHS = List.of("/auth/login","/auth/register", "/reference","/reports/public");


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
			if (!hasAccess(path, role, method, internalCall, userId)) {
				exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
				return exchange.getResponse().setComplete();
			}

			// forward headers to services
			ServerWebExchange mutatedExchange = exchange.mutate()
					.request(builder -> builder.header("X-USER-ID", userId).header("X-USER-ROLE", role)).build();
			return chain.filter(mutatedExchange);
		} 
		catch (Exception ex) {
			exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
			return exchange.getResponse().setComplete();
		}
	}

	//role->route map  (role based security)
	private boolean hasAccess(String path, String role, String method,String internalCall, String tokenUserId) {

		// assign grievance
		if (path.matches(".*/grievances/.*/assign") && method.equals(METHOD_PUT)) {
			return role.equals(ROLE_ADMIN) || role.equals(ROLE_SUPERVISOR);
		}

		// escalate grievance
		if (path.matches(".*/grievances/.*/escalate") && method.equals(METHOD_PUT)) {
			return role.equals(ROLE_CITIZEN);
		}

        //officer-only 
        if ((path.matches(".*/grievances/.*/in-review")|| path.matches(".*/grievances/.*/resolve")) && method.equals(METHOD_PUT)) {
			return role.equals(ROLE_OFFICER);
		}

        //close grievance
		if (path.matches(".*/grievances/.*/close") && method.equals(METHOD_PUT)) {
            return role.equals(ROLE_ADMIN)|| role.equals(ROLE_SUPERVISOR)|| role.equals(ROLE_OFFICER);
		}

		// specific: user summary report (allow CITIZEN, OFFICER, SUPERVISOR, ADMIN)
		if (path.startsWith("/reports/user/") && method.equals(METHOD_GET)) {
			return role.equals(ROLE_ADMIN) || role.equals(ROLE_SUPERVISOR) || role.equals(ROLE_OFFICER) || role.equals(ROLE_CITIZEN);
		}
        //reports
		if (path.startsWith("/reports") && method.equals(METHOD_GET)) {
			return role.equals(ROLE_ADMIN) || role.equals(ROLE_SUPERVISOR) || role.equals(ROLE_OFFICER);
		}

		if (path.startsWith("/users/supervisor/department") && method.equals(METHOD_GET)) {
			return "true".equalsIgnoreCase(internalCall); // trust internal call
		}

		if (path.matches("/users/[^/]+$") && method.equals(METHOD_GET)) {
            if ("true".equalsIgnoreCase(internalCall)) return true;
            if (role.equals(ROLE_ADMIN)) return true;
            String pathId = path.substring(path.lastIndexOf('/') + 1);
            return pathId.equals(tokenUserId);
        }

		// update own profile
		if (path.matches("/users/[^/]+$") && method.equals(METHOD_PUT)) {
            return role.equals(ROLE_CITIZEN) || role.equals(ROLE_OFFICER)|| role.equals(ROLE_SUPERVISOR) || role.equals(ROLE_ADMIN);
		}

		// admin-only operations
		if (path.matches("/users/.*/role/.*") && method.equals(METHOD_PUT)
				|| path.matches("/users/.*/department/.*") && method.equals(METHOD_PUT)
				|| path.startsWith("/users/role/") && method.equals(METHOD_PUT)) {
			return role.equals(ROLE_ADMIN);
		}

		// default -- authenticated user allowed
		return true;
	}

}
