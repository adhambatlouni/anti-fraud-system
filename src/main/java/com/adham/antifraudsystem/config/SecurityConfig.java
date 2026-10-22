package com.adham.antifraudsystem.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public UserDetailsService userDetailsService(DataSource dataSource) {
        return new JdbcUserDetailsManager(dataSource);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        return http
                .httpBasic(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        ))
                .authorizeHttpRequests(auth -> auth

                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()

                        // Public registration
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/user"
                        )
                        .permitAll()

                        // Public container readiness endpoint
                        .requestMatchers(
                                HttpMethod.GET,
                                "/actuator/health",
                                "/actuator/health/**"
                        )
                        .permitAll()

                        // Public API documentation
                        .requestMatchers(
                                HttpMethod.GET,
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**"
                        )
                        .permitAll()

                        // Administrator endpoints
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/auth/user/*"
                        )
                        .hasRole("ADMINISTRATOR")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/auth/role"
                        )
                        .hasRole("ADMINISTRATOR")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/auth/access"
                        )
                        .hasRole("ADMINISTRATOR")

                        // Administrator and Support
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/list"
                        )
                        .hasAnyRole("ADMINISTRATOR", "SUPPORT")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/antifraud/audit"
                        )
                        .hasAnyRole("ADMINISTRATOR", "SUPPORT")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v2/antifraud/metrics"
                        )
                        .hasAnyRole("ADMINISTRATOR", "SUPPORT")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/antifraud/suspicious-ip"
                        )
                        .hasRole("SUPPORT")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/antifraud/suspicious-ip"
                        )
                        .hasRole("SUPPORT")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/antifraud/suspicious-ip/*"
                        )
                        .hasRole("SUPPORT")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/antifraud/stolencard"
                        )
                        .hasRole("SUPPORT")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/antifraud/stolencard"
                        )
                        .hasRole("SUPPORT")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/antifraud/stolencard/*"
                        )
                        .hasRole("SUPPORT")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/antifraud/history",
                                "/api/antifraud/history/*"
                        )
                        .hasRole("SUPPORT")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v2/antifraud/transactions"
                        )
                        .hasRole("SUPPORT")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v2/antifraud/transactions/*/decision"
                        )
                        .hasRole("SUPPORT")

                        // Support: transaction feedback
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/antifraud/transaction"
                        )
                        .hasRole("SUPPORT")

                        // Merchant: evaluate transactions
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/antifraud/transaction"
                        )
                        .hasRole("MERCHANT")

                        .anyRequest().authenticated()
                )
                .build();
    }
}
