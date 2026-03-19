package com.learncicd.authservice.config;

import com.learncicd.authservice.repository.UserRepo;
import com.learncicd.authservice.service.MyUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final UserRepo userRepo;

    @Bean
    public PasswordEncoder passwordEncoder(){
        log.debug("SecurityConfig: passwordEncoder Bean created");
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception{
        httpSecurity
                //.csrf(httpSecurityCsrfConfigurer -> httpSecurityCsrfConfigurer.disable())
                .csrf(AbstractHttpConfigurer::disable)
                //.authorizeHttpRequests(authorizationManagerRequestMatcherRegistry -> )
                .authorizeHttpRequests(req -> {
                            req.requestMatchers("/auth/register-user","/auth/generate-token").permitAll();
                            req.anyRequest().authenticated();
                        }).userDetailsService(userDetailsService())
                .httpBasic(Customizer.withDefaults())
                .formLogin(Customizer.withDefaults());
        log.debug("SecurityConfig: filterChain Bean created");
        return httpSecurity.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config){
        log.debug("SecurityConfig: authenticationManager Bean created");
        return config.getAuthenticationManager();
    }

    @Bean
    public UserDetailsService userDetailsService(){
        log.debug("SecurityConfig: userDetailsService Bean created");
        return new MyUserDetailsService(userRepo);
    }
}
