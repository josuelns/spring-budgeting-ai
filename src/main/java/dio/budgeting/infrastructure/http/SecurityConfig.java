package dio.budgeting.infrastructure.http;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configura o Resource Server OAuth2 integrado ao Keycloak.
 *
 * <p>Todos os endpoints exigem um JWT válido emitido pelo realm "bank-assistant".
 * O issuer-uri está configurado em application.yml.
 *
 * <p>O "sub" do JWT (keycloakUserId) é o valor retornado por
 * SecurityContextHolder.getContext().getAuthentication().getName()
 * — usado pelo BankOperationsUseCase para identificar o usuário.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults()))
                .build();
    }
}
