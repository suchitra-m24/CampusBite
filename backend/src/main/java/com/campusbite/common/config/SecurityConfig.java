package com.campusbite.common.config;

import com.campusbite.common.security.CustomUserDetailsService;
import com.campusbite.common.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PasswordEncoder passwordEncoder;
    private final UrlBasedCorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                           JwtAuthenticationFilter jwtAuthenticationFilter,
                           PasswordEncoder passwordEncoder,
                           UrlBasedCorsConfigurationSource corsConfigurationSource) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.passwordEncoder = passwordEncoder;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/auth/register", "/api/auth/login",
                                "/api/auth/refresh", "/api/auth/logout",
                                "/api/auth/verify-email", "/api/auth/forgot-password",
                                "/api/auth/reset-password").permitAll()
                        .requestMatchers("/ws/**", "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/menu/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/kitchen-load/current").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/push/vapid-public-key").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/broadcast/active").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/staff/**").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers("/api/student/wallet/**").hasRole("STUDENT")
                        .requestMatchers("/api/student/**").hasAnyRole("STUDENT", "TEACHER")
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
