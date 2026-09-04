package dio.budgeting.infrastructure.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Serviço de idempotência baseado em Redis para operações financeiras críticas.
 *
 * <h3>Problema que resolve</h3>
 * <p>Em um assistente de voz bancário, o mesmo saque pode chegar duas vezes ao servidor por:
 * <ul>
 *   <li>Retry automático da camada de rede (TCP timeout, HTTP retry do cliente)</li>
 *   <li>O modelo de IA reenviar o @Tool por não receber resposta a tempo</li>
 *   <li>O usuário repetir o mesmo comando de voz rapidamente</li>
 * </ul>
 *
 * <h3>Estratégia: Lock Redis + Release on Failure</h3>
 * <pre>
 * Requisição A (legítima):
 *   1. acquireLock("withdraw:user:100.00") → Redis SETNX → true (lock adquirido)
 *   2. Executa o saque no MySQL
 *   3. Sucesso → TTL expira naturalmente em 15s
 *
 * Requisição B (duplicata — chega 3s depois):
 *   1. acquireLock("withdraw:user:100.00") → Redis SETNX → false (chave já existe)
 *   2. Retorna imediatamente: "Operação já processada"
 *   3. Saque NÃO é executado novamente ✓
 *
 * Requisição A (falha — MySQL fora do ar):
 *   1. acquireLock → true
 *   2. MySQL lança exceção → catch no use case → releaseLock()
 *   3. Chave Redis deletada → usuário pode tentar novamente imediatamente ✓
 * </pre>
 *
 * <h3>Combinação com Lock Pessimista MySQL</h3>
 * <p>Esta camada Redis atua ANTES da consulta ao MySQL (pré-filtro rápido).
 * O {@code SELECT FOR UPDATE} do MySQL é a garantia ACID para os casos
 * em que dois requests passam simultaneamente pelo Redis (extremamente improvável
 * dado que o SETNX é atômico, mas defensivamente correto ter as duas camadas).
 */
@Component
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;

    /** Janela de proteção: 15 segundos é suficiente para bloquear retries automáticos
     *  e duplicatas por voz sem prejudicar retentativas legítimas do usuário. */
    private static final Duration LOCK_TTL = Duration.ofSeconds(15);

    private static final String KEY_PREFIX = "voicebank:idempotency:";

    public IdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Tenta adquirir o lock de idempotência para a operação.
     *
     * <p>Usa Redis {@code SETNX + TTL} (operação atômica) — não há janela de race condition.
     *
     * @return {@code true} se o lock foi adquirido (operação inédita → prosseguir),
     *         {@code false} se a operação já está em andamento ou foi executada recentemente (duplicata)
     */
    public boolean acquireLock(String userId, String operation, String... params) {
        String key = buildKey(userId, operation, params);
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * Libera o lock em caso de falha, permitindo que o usuário tente novamente imediatamente.
     *
     * <p>Deve ser chamado APENAS no bloco {@code catch} — nunca em caso de sucesso,
     * pois o TTL natural protege contra duplicatas até expirar.
     */
    public void releaseLock(String userId, String operation, String... params) {
        redisTemplate.delete(buildKey(userId, operation, params));
    }

    private String buildKey(String userId, String operation, String... params) {
        return KEY_PREFIX + userId + ":" + operation + ":" + String.join(":", params);
    }
}
