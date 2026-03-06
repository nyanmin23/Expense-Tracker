package dev.jade.expensetracker.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

/**
 * Custom request filter that intercepts every HTTP request to perform JWT-based authentication.
 *
 * <p>This filter executes once per request (via {@link OncePerRequestFilter}), extracts
 * a JWT from the Authorization header if present, validates it, and populates the
 * Spring Security context with an authenticated {@link UsernamePasswordAuthenticationToken}
 * when a valid token and user identity are found.</p>
 *
 * <p>By registering this filter before
 * {@code UsernamePasswordAuthenticationFilter} in the security filter chain, it ensures
 * that the user’s identity is established before Spring Security performs authorization
 * checks. If no JWT is present or the token is invalid, the filter does nothing beyond
 * allowing the request to continue through the filter chain. Other filters (such as
 * {@code AnonymousAuthenticationFilter}) will handle unauthenticated requests where
 * appropriate. :contentReference[oaicite:1]{index=1}</p>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    /**
     * HandlerExceptionResolver bridges the gap between the filter layer and
     * Spring MVC's exception handling. Without this, exceptions thrown in filters
     * bypass GlobalExceptionHandler entirely and return ugly default error responses.
     * Spring auto-qualifies the "handlerExceptionResolver" bean name specifically —
     * it must match exactly or Spring injects the wrong resolver.
     */
    private final HandlerExceptionResolver handlerExceptionResolver;

    public JwtAuthFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    /**
     * Core filter method that runs once per HTTP request.
     *
     * <p>This method extracts the Authorization header and checks for a Bearer token.
     * If the header is missing or does not begin with “Bearer ”, the filter immediately
     * delegates to the next filter by calling {@code filterChain.doFilter(request, response)}.</p>
     *
     * <p>If a token is present, it strips the “Bearer ” prefix, extracts the username
     * claim from the token, and verifies whether there is no existing-authenticated
     * principal in {@code SecurityContextHolder} for the current request thread.</p>
     *
     * <p>When the username is non-null and no authentication is yet set, the filter
     * loads user details using {@code UserDetailsService}. It then validates the token
     * against the loaded user details using {@code jwtService.isTokenValid}. If valid,
     * it constructs a {@code UsernamePasswordAuthenticationToken}, sets request
     * details into it, and places it into the current security context
     * ({@code SecurityContextHolder.getContext()}). After processing, the
     * filter always continues the filter chain so that subsequent filters and the
     * dispatcher servlet can act on the request. :contentReference[oaicite:2]{index=2}
     *
     * @param request     current HTTP request
     * @param response    current HTTP response
     * @param filterChain the filter chain that continues processing
     * @throws ServletException if an internal servlet error occurs
     * @throws IOException      if an I/O error occurs while reading the request
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        /*
         Wrapping everything in try-catch ensures that any exception thrown during
         JWT processing (expired token, malformed token, invalid signature etc.)
         is forwarded to GlobalExceptionHandler via handlerExceptionResolver
         instead of returning Spring Security's default unformatted error response.
        */
        try {

            /*
             Reads the "Authorization" header from the incoming request.
             Expected format: "Bearer eyJhbGci..."
            */
            String authHeader = request.getHeader("Authorization");

            /*
             If the header is absent or doesn't follow the Bearer format,
             skip JWT processing entirely and pass the request to the next filter.
             This handles public routes like /api/auth/login cleanly —
             they have no token so we just let them through without any processing.
            */
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return; // stop executing this filter — no token to process
            }

            /*
             Strip the "Bearer " prefix (7 characters) to isolate the raw JWT string.
             e.g. "Bearer eyJhbGci..." → "eyJhbGci..."
            */
            String token = authHeader.split("Bearer ")[1];

            /*
             Extract the username (email) from the token's "sub" (subject) claim.
             If the token is malformed or has an invalid signature, jjwt throws
             a JwtException here which is caught by the catch block below.
            */
            String username = jwtService.extractUsername(token);

            /*
             Only proceed if:
             1. username is not null — token had a valid subject claim
             2. no authentication is already set in the SecurityContext —
                avoids redundant processing if the user is already authenticated
                earlier in the filter chain for this request
            */
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                /*
                 Load the full UserDetails object from the database using the email.
                 This gives us the user's authorities and account status for validation.
                */
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                /*
                 Validate the token — checks two things:
                 1. token subject matches the loaded user's username
                 2. token has not expired
                */
                if (jwtService.isTokenValid(token, userDetails)) {

                    /*
                     Construct Spring Security's authentication token representing
                     a successfully authenticated user.
                     param 1 (principal)   — the authenticated user (UserDetails)
                     param 2 (credentials) — null because we don't need the password
                                             post-authentication
                     param 3 (authorities) — the user's roles/permissions used by
                                             Spring Security for authorization decisions
                    */
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    /*
                     Attach additional HTTP request details to the authentication object
                     such as the remote IP address and session ID.
                     WebAuthenticationDetailsSource — creates these details from the request
                     buildDetails(request)          — extracts and wraps the request metadata
                    */
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    /*
                     Store the authenticated principal in the SecurityContext.
                     This is the key line — it tells Spring Security "this request
                     is authenticated". After this point, authorization rules in
                     SecurityConfig evaluate against this authentication object
                     and allow or deny access to the requested endpoint.
                    */
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            /*
             Always pass the request to the next filter in the chain regardless
             of whether authentication succeeded or not. If authentication was not
             set, Spring Security's authorization filter will handle the denial.
            */
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            /*
             Forward any exception to GlobalExceptionHandler via HandlerExceptionResolver.
             param 1 (request)  — needed to write the error response
             param 2 (response) — needed to write the error response
             param 3 (handler)  — the controller handler, null here since
                                  we're in a filter outside the MVC context
             param 4 (ex)       — the exception to be handled
            */
            handlerExceptionResolver.resolveException(request, response, null, e);
        }
    }
}
