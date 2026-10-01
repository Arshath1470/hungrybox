package com.hungrybox.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Verifies the bearer token on every non-public request and exposes the caller
 * as a request attribute, which controllers receive through {@link CurrentUser}.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String USER_ATTRIBUTE = "hungrybox.authenticatedUser";

    private static final List<String> ALWAYS_PUBLIC = List.of("/actuator/**", "/error");

    private final JwtService jwtService;
    private final List<PublicPath> publicPaths;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public JwtAuthFilter(JwtService jwtService, List<String> publicPaths) {
        this.jwtService = jwtService;
        this.publicPaths = publicPaths.stream().map(PublicPath::parse).toList();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublic(request.getMethod(), request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            reject(response, "Missing bearer token");
            return;
        }

        try {
            AuthenticatedUser user = jwtService.verify(header.substring("Bearer ".length()).trim());
            request.setAttribute(USER_ATTRIBUTE, user);
        } catch (JwtService.InvalidTokenException e) {
            reject(response, e.getMessage());
            return;
        }
        chain.doFilter(request, response);
    }

    boolean isPublic(String method, String path) {
        for (String pattern : ALWAYS_PUBLIC) {
            if (matcher.match(pattern, path)) {
                return true;
            }
        }
        for (PublicPath publicPath : publicPaths) {
            boolean methodMatches = publicPath.method() == null || publicPath.method().equalsIgnoreCase(method);
            if (methodMatches && matcher.match(publicPath.pattern(), path)) {
                return true;
            }
        }
        return false;
    }

    private static void reject(HttpServletResponse response, String detail) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/problem+json");
        response.getWriter().write(
                "{\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"" + detail.replace("\"", "'") + "\"}");
    }

    /** "GET /api/restaurants/**" or "/actuator/**" (any method). */
    record PublicPath(String method, String pattern) {
        static PublicPath parse(String raw) {
            String value = raw.trim();
            int space = value.indexOf(' ');
            if (space < 0) {
                return new PublicPath(null, value);
            }
            return new PublicPath(value.substring(0, space).toUpperCase(), value.substring(space + 1).trim());
        }
    }
}
