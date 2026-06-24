package com.example.shop.config;

import com.example.shop.security.JWTFilter;
import com.example.shop.service.impl.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class securityConfig {
    @Autowired
    private JWTFilter jwtFilter;
    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**") // Chỉ áp dụng bộ lọc bảo mật cho các đường dẫn /api/**
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults()) // bật CORS
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers(AppConstants.PUBLIC_URLS).permitAll()

                        .requestMatchers("/api/auth/me").authenticated()

                        .requestMatchers(AppConstants.ADMIN_URL).hasAnyAuthority(AppConstants.POSITION_ADMIN)

                        .requestMatchers(AppConstants.EMPLOYEE_MANA_URL)
                        .hasAnyAuthority(AppConstants.POSITION_ADMIN, AppConstants.POSITION_MANAGER)

                        .requestMatchers(AppConstants.EMPLOYEE_STAFF_URL)
                        .hasAnyAuthority(AppConstants.POSITION_MANAGER, AppConstants.POSITION_STAFF, AppConstants.POSITION_ADMIN)
                        .requestMatchers(AppConstants.SHARED_URL).authenticated()
                        .anyRequest().authenticated()
                )

                .exceptionHandling(handling -> handling.authenticationEntryPoint(
                        (request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Bạn cần đăng nhập để sử dụng chức năng này")
                ))
                .sessionManagement(management ->
                        management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        http.authenticationProvider(daoAuthenticationProvider());

        return http.build();// Trả về trực tiếp, không cần biến trung gian
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();

        // Cho phép React (Vite) truy cập API
        configuration.setAllowedOrigins(java.util.List.of("http://localhost:3000", "http://localhost:5173"));

        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source =
                new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

}
