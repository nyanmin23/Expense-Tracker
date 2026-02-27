package dev.jade.expensetracker.security;

import dev.jade.expensetracker.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;
    private final JwtAuthFilter jwtAuthFilter;

    /**
     * Configures the Spring Security filter chain for a stateless, JWT-based authentication system.
     *
     * <p>This method defines how HTTP security is applied to the application. It customizes:
     * CSRF handling, session management policy, endpoint authorization rules, and the
     * insertion of a custom JWT authentication filter into the Spring Security filter chain.</p>
     *
     * <p><b>Architectural Context:</b></p>
     * <ul>
     *     <li>The application uses token-based authentication (JWT) instead of session-based authentication.</li>
     *     <li>No HTTP session is created or stored on the server.</li>
     *     <li>Each request must independently carry authentication credentials (typically in the Authorization header).</li>
     * </ul>
     *
     * <p><b>Execution Flow:</b></p>
     * <ol>
     *     <li>The client sends an HTTP request.</li>
     *     <li>The custom JwtAuthFilter intercepts the request before credential-based authentication occurs.</li>
     *     <li>If a valid JWT is present, the SecurityContext is populated.</li>
     *     <li>Authorization rules determine whether access is granted.</li>
     * </ol>
     *
     * @param http the {@link HttpSecurity} builder used to configure web-based security
     * @return a fully constructed {@link SecurityFilterChain} defining the security behavior
     * @throws Exception if a configuration error occurs during filter chain construction
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http

                /*
                  Disables Cross-Site Request Forgery (CSRF) protection.

                  CSRF protection is primarily required for session-based authentication
                  where browsers automatically attach cookies to requests.

                  In a stateless JWT architecture:
                  - Authentication data is explicitly sent in the Authorization header.
                  - The server does not rely on cookies for authentication state.

                  Therefore, CSRF protection is unnecessary and explicitly disabled.
                 */
                .csrf(AbstractHttpConfigurer::disable)

                /*
                  Configures session management to be stateless.

                  SessionCreationPolicy.STATELESS ensures:
                  - No HTTP session is created.
                  - No existing session is used.
                  - SecurityContext is not stored between requests.

                  Each request must independently authenticate using JWT.
                 */
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                /*
                  Defines authorization rules for incoming HTTP requests.

                  requestMatchers("/api/auth/**").permitAll():
                  - Allows unrestricted access to authentication-related endpoints
                    (e.g., login, registration).

                  anyRequest().authenticated():
                  - All other endpoints require a successfully authenticated user.
                  - If no valid Authentication object is present in the SecurityContext,
                    access is denied.
                 */
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated())

                /*
                  Inserts the custom JWT authentication filter into the filter chain.

                  addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class):

                  - Ensures JwtAuthFilter executes before the standard
                    UsernamePasswordAuthenticationFilter.
                  - JwtAuthFilter extracts the JWT from the Authorization header.
                  - Validates the token.
                  - If valid, constructs an Authentication object and sets it in the
                    SecurityContext.

                  Positioning is critical: the filter must run early enough to establish
                  authentication before authorization rules are evaluated.
                 */
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        /*
          Builds and returns the configured SecurityFilterChain.

          http.build() finalizes the configuration and produces an immutable
          SecurityFilterChain instance that Spring Boot registers in the
          application context.
         */
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

}
