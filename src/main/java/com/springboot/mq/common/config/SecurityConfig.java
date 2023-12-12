package com.springboot.mq.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Profile("!test")
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf().disable()
                .httpBasic().disable()
                .cors().and()
                .authorizeRequests()
                .antMatchers(
                        "/kafka/**","/actuator/health", "/users/**"
                ).permitAll()
                .anyRequest().authenticated();

            return http.build();
    }
}
