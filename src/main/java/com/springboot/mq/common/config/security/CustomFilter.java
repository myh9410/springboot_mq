package com.springboot.mq.common.config.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

@Slf4j
@Configuration
public class CustomFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            if (Objects.isNull(request.getHeader("some-header"))) {
                log.warn("no header :: some-header");
            } else {
                //do something
            }
        } catch (Exception e) {
            log.error("customFilter Error :: {}", e.getMessage());
        } finally {
            filterChain.doFilter(request, response);
        }
    }
}
