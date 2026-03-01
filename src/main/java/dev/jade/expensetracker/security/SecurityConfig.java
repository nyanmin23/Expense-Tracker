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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

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
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

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

    /**
     * Exposes the core Spring Security {@code AuthenticationManager} as a bean.
     *
     * <p>In Spring Security, the {@code AuthenticationManager} is the central component
     * responsible for authenticating credentials. It acts as the mechanism invoked by
     * authentication filters (for example, those handling form login or basic authentication)
     * to process an {@code Authentication} request, delegate to one or more
     * {@code AuthenticationProvider} instances, and return a fully authenticated
     * {@code Authentication} on success. {@code ProviderManager} is the most common
     * concrete implementation used behind the scenes. {@code AuthenticationManager}
     * implementations are ultimately responsible for verifying credentials and
     * populating the {@code SecurityContextHolder} with authentication results. {@code turn0search0}{@code turn0search2}</p>
     *
     * <p>Because Spring Security no longer provides the {@code WebSecurityConfigurerAdapter}
     * by default, the framework auto-configures an {@code AuthenticationManager} based on
     * beans such as {@code UserDetailsService} and {@code PasswordEncoder}. Those
     * components are used by the default authentication providers to build the manager.
     * The {@code AuthenticationConfiguration} class captures that internally built
     * manager and allows it to be exposed as a bean. Without this explicit bean
     * declaration, the auto-configured {@code AuthenticationManager} would not be
     * available for injection into other beans (for example, custom filters or services
     * that programmatically authenticate credentials).
     *
     * <p>In summary, this method fetches the already configured {@code AuthenticationManager}
     * from Spring Security’s internal configuration and makes it a Spring bean so it can be
     * injected elsewhere. It does not construct the manager manually, but defers to
     * {@code AuthenticationConfiguration.getAuthenticationManager()} to return the
     * correct instance. This pattern replaces the older
     * {@code authenticationManagerBean()} pattern from the deprecated
     * {@code WebSecurityConfigurerAdapter}.
     *
     * @param config the Spring Security {@code AuthenticationConfiguration} that holds
     *               the auto-configured {@code AuthenticationManager}
     * @return the {@code AuthenticationManager} that Spring Security has built
     * @throws Exception if the {@code AuthenticationManager} cannot be retrieved
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("http://localhost:*")); // frontend URL
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
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
