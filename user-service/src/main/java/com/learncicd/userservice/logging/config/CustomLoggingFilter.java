package com.learncicd.userservice.logging.config;

import com.learncicd.userservice.exception.CustomException;
import jakarta.annotation.Nullable;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 *  intercept exceptions before they hit Tomcat’s default logging:
 *
 * - Use a logging filter ::
 * You can add a LoggingFilter or customize DefaultErrorAttributes to suppress stack traces for specific exception types.
 *
 * - Logback configuration  ::
 * In logback-spring.xml, you can configure appenders to filter out stack traces for CustomException while still logging them for unexpected errors.
 * This way, when a CustomException is thrown, only the message is logged at WARN level, without the full stack trace. Other exceptions will still log normally.
 */
//@Component
//@Slf4j
//public class CustomLoggingFilter extends OncePerRequestFilter {
//    @Override
//    protected void doFilterInternal(@Nullable HttpServletRequest request,
//                                    @Nullable HttpServletResponse response,
//                                    FilterChain filterChain)
//            throws ServletException, IOException {
//        try {
//            filterChain.doFilter(request, response);
//        } catch (CustomException ce) {
//            // Log only the message, no stack trace
//            log.warn("Business error: {}", ce.getMessage());
//            throw ce; // rethrow so GlobalExceptionHandler formats response
//        }
//    }
//}

