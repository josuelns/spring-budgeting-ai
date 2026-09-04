package dio.budgeting.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Configuração de suporte a métodos assíncronos com Virtual Threads (Project Loom).
 *
 * <p>Por que Virtual Threads e não um ThreadPoolExecutor tradicional?
 * <ul>
 *   <li>Virtual Threads são leves (KB vs MB de stack) — podem existir milhões simultâneas.</li>
 *   <li>Cada task {@code @Async} cria uma nova virtual thread sem bloquear a platform thread.</li>
 *   <li>Comportamento similar às Promises/async-await do JavaScript: fire-and-forget eficiente.</li>
 *   <li>Não há thread pool para dimensionar — o JVM gerencia o scheduling automaticamente.</li>
 * </ul>
 *
 * <p>Casos de uso ideais com {@code @Async} + Virtual Threads neste projeto:
 * <ul>
 *   <li>Envio de notificações após depósito/transferência.</li>
 *   <li>Geração de relatórios financeiros em background.</li>
 *   <li>Chamadas a serviços externos (e-mail, SMS, webhooks) sem bloquear a resposta ao usuário.</li>
 * </ul>
 *
 * <p>O bean é nomeado {@code "taskExecutor"} — convenção reconhecida pelo Spring para
 * ser usado como executor padrão de {@code @Async} quando não especificado na anotação.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor virtualThreadExecutor() {
        // newVirtualThreadPerTaskExecutor(): cria uma nova virtual thread para CADA task submetida.
        // Não há pool — virtual threads são tão baratas que criar e descartar é mais eficiente
        // do que reutilizar (ao contrário das platform threads).
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
