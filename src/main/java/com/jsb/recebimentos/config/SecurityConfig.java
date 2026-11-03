package com.jsb.recebimentos.config;

import com.jsb.recebimentos.security.LoginSuccessHandler;
import com.jsb.recebimentos.security.MfaEnforcementFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain applicationSecurity(HttpSecurity http, LoginSuccessHandler loginSuccessHandler) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/setup", "/mfa", "/error", "/images/**", "/favicon.ico").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(loginSuccessHandler)
                        .failureUrl("/login?error"))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID"))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.migrateSession()));
        http.addFilterAfter(new MfaEnforcementFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
