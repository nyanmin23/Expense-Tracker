package dev.jade.expensetracker.domain.auth;

import dev.jade.expensetracker.common.DuplicateResourceException;
import dev.jade.expensetracker.common.ResourceNotFoundException;
import dev.jade.expensetracker.domain.auth.dto.AuthResponse;
import dev.jade.expensetracker.domain.auth.dto.LoginRequest;
import dev.jade.expensetracker.domain.auth.dto.RegisterUserRequest;
import dev.jade.expensetracker.domain.user.User;
import dev.jade.expensetracker.domain.user.UserRepository;
import dev.jade.expensetracker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterUserRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already in use");
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser);

        return new AuthResponse(savedUser.getUserId(), savedUser.getEmail(), savedUser.getCreatedAt(), token);
    }

    public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = jwtService.generateToken(user);

        return new AuthResponse(user.getUserId(), user.getEmail(), user.getCreatedAt(), token);
    }
}
