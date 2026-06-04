package com.example.library_management.security;

import com.example.library_management.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    @Bean
    public BCryptPasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserService userService){
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity,LoginSuccessHandler successHandler) throws Exception {
        httpSecurity.authorizeHttpRequests(
                req -> req
                        .requestMatchers("/quan-ly/**")
                        .hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers("/home").permitAll()
                        .requestMatchers("/assets/**").permitAll()
                        .anyRequest().authenticated()
        ).formLogin(
                login -> login.loginPage("/login")
                        .loginProcessingUrl("/authenticateTheUser")
                        .successHandler(successHandler)
                        .permitAll()
        ).logout(
                logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
        ).exceptionHandling(
                exception->exception
                        .accessDeniedPage("/showPage403")

        );
        httpSecurity.csrf(csrf->csrf.disable());
        return httpSecurity.build();
    }
}
