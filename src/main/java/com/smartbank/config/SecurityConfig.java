package com.smartbank.config;

import com.smartbank.service.impl.CustomUserDetailsService;
import com.smartbank.security.JwtAuthenticationFilter;
import com.smartbank.security.JwtUtil;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    public SecurityConfig(CustomUserDetailsService userDetailsService, JwtUtil jwtUtil) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/accounts/**").hasAnyAuthority("ADMIN")
                        .requestMatchers("/api/v1/transactions/**").hasAnyAuthority("USER")
                        // if you want specific endpoints read-only vs admin-only, adjust here
                        .anyRequest().authenticated()
                )
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Expose AuthenticationManager for login endpoint if needed
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
/*
⚙️ Full Flow of a Secured API Request (with JWT)

Client sends HTTP request with JWT token
Authorization: Bearer <token>

JwtAuthenticationFilter intercepts the request
✅ Validate token
✅ Extract user & roles
✅ Set Authentication in SecurityContext

Authorization rules check
Does user have permission for this URL?

If authorized → Controller runs
If not → 401/403
 */