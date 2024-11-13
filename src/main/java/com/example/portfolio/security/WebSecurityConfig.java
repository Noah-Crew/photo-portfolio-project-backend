package com.example.portfolio.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class WebSecurityConfig {
	
	private final LoginFailureHandler loginFailureHandler;
	
	public WebSecurityConfig(LoginFailureHandler loginFailureHandler) {
		this.loginFailureHandler = loginFailureHandler;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
	    // csrf 비활성화
	    .csrf(csrf -> csrf.disable())
	    // H2 콘솔을 위한 헤더 설정
	    .headers(headers -> headers
	        .contentSecurityPolicy(csp -> csp
	            .policyDirectives("frame-ancestors 'self'")
	        )
	    )
	    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
	    .authorizeHttpRequests(auth -> auth
	        // H2 콘솔에 대한 접근 허용
	        .requestMatchers("/h2-console/**").permitAll()
	        // 특정 요청 보안 설정
	        .requestMatchers("/admin/**").authenticated()
	        // permitAll() : 인증 없이 접근 허용 
	        .anyRequest().permitAll()	
	    )
	    .formLogin(form -> form
	        // 로그인 성공 시 이동할 url
	    		.loginPage("/admin/login")
				.loginProcessingUrl("/loginProcess")
				.usernameParameter("id")
				.defaultSuccessUrl("/api/loginSucess")
				.failureHandler(loginFailureHandler)
				.permitAll()
	    );

	return http.build();
	}
	
	// CORS 설정
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowCredentials(true); // 쿠키 허용
		config.setAllowedOrigins(List.of("https://minography.vercel.app/")); // 프론트 port
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")); // 허용 메소드
		config.setAllowedHeaders(List.of("*"));
		
		// url 경로마다 cors 적용 가능
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config); // 동일하게 적용 
		return source;
	}

	// 암호화 알고리즘
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
