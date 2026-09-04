package dio.budgeting.application;

import dio.budgeting.application.output.BillPlanningDto;
import dio.budgeting.application.output.TransactionDto;
import dio.budgeting.domain.AccountRepository;
import dio.budgeting.domain.BankTransactionRepository;
import dio.budgeting.domain.BillPlanning;
import dio.budgeting.domain.BillPlanningRepository;
import dio.budgeting.domain.BillPlanningStatus;
import dio.budgeting.infrastructure.mapper.BillPlanningMapper;
import dio.budgeting.infrastructure.mapper.TransactionMapper;
import dio.budgeting.infrastructure.service.IdempotencyService;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Caso de uso do VoiceBank — AI Personal Finance Management.
 *
 * <h3>Proteção contra double-spending (saques/pagamentos duplicados)</h3>
 * <p>Operações financeiras destrutivas usam duas camadas de proteção:
 *
 * <pre>
 * Camada 1 — Redis (IdempotencyService):
 *   SETNX atômico com TTL de 15s → bloqueia retries de rede, retries da IA e
 *   comandos de voz duplicados antes mesmo de tocar no banco de dados.
 *   Em caso de falha, o lock é liberado para permitir nova tentativa.
 *
 * Camada 2 — MySQL Pessimistic Lock (SELECT FOR UPDATE):
 *   Garante consistência ACID para requisições que passem simultaneamente
 *   pelo Redis. Serializa escritas na mesma linha da tabela accounts.
 *
 * Camada 3 — Regra de domínio (Account.withdraw):
 *   Valida balance >= amount. Lança InsufficientBalanceException se insuficiente.
 * </pre>
 *
 * <p>O usuário é identificado EXCLUSIVAMENTE pelo JWT: nenhum @Tool aceita
 * parâmetro de "conta de origem" ou "userId".
 */
@Service
public class BankOperationsUseCase {

    private final AccountRepository accountRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final BillPlanningRepository billPlanningRepository;
    private final TransactionMapper transactionMapper;
    private final BillPlanningMapper billPlanningMapper;
    private final IdempotencyService idempotencyService;

    public BankOperationsUseCase(AccountRepository accountRepository,
                                 BankTransactionRepository bankTransactionRepository,
                                 BillPlanningRepository billPlanningRepository,
                                 TransactionMapper transactionMapper,
                                 BillPlanningMapper billPlanningMapper,
                                 IdempotencyService idempotencyService) {
        this.accountRepository = accountRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.billPlanningRepository = billPlanningRepository;
        this.transactionMapper = transactionMapper;
        this.billPlanningMapper = billPlanningMapper;
        this.idempotencyService = idempotencyService;
    }

    // ═══════════════════════════════════════════════════════
    // EXTRATO BANCÁRIO
    // ═══════════════════════════════════════════════════════

    @Tool(name = "get-statement",
          description = "Consulta o extrato bancário com todas as movimentações da conta do usuário autenticado")
    @Transactional(readOnly = true)
    public List<TransactionDto> getStatement() {
        var userId = resolveUserId();
        var account = accountRepository.findByKeycloakUserId(userId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada para o usuário autenticado"));
        return transactionMapper.toDto(bankTransactionRepository.findByAccountId(account.getId()));
    }

    // ═══════════════════════════════════════════════════════
    // DEPÓSITO
    // Camadas: Redis idempotência + @Retry para falhas transitórias de conexão MySQL
    // ═══════════════════════════════════════════════════════

    @Tool(name = "deposit",
          description = "Realiza um depósito em reais na conta do usuário autenticado")
    @Retry(name = "depositRetry")
    @Transactional
    public String deposit(@ToolParam(description = "Valor a depositar em reais (ex: 150.00)") BigDecimal amount) {
        var userId = resolveUserId();
        var amountKey = amount.toPlainString();

        // Camada 1 — Redis: impede depósito duplicado por retry do @Retry ou da rede
        if (!idempotencyService.acquireLock(userId, "deposit", amountKey)) {
            return "Depósito de R$ " + fmt(amount) + " já está sendo processado. " +
                   "Aguarde alguns instantes e consulte seu extrato para confirmar.";
        }

        try {
            var account = accountRepository.findByKeycloakUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Conta não encontrada para o usuário autenticado"));

            var transaction = account.deposit(amount);
            accountRepository.save(account);
            bankTransactionRepository.save(transaction, account.getId());

            return "Depósito de R$ " + fmt(amount) + " realizado com sucesso. " +
                   "Saldo atual: R$ " + fmt(account.getBalance());

        } catch (Exception e) {
            // Libera o lock Redis para que o @Retry possa tentar novamente
            idempotencyService.releaseLock(userId, "deposit", amountKey);
            throw e;
        }
    }

    // ═══════════════════════════════════════════════════════
    // SAQUE
    // Camadas: Redis idempotência + MySQL Lock Pessimista + regra de domínio
    // ═══════════════════════════════════════════════════════

    @Tool(name = "withdraw",
          description = "Realiza um saque em reais na conta do usuário autenticado")
    @Transactional
    public String withdraw(@ToolParam(description = "Valor a sacar em reais (ex: 200.00)") BigDecimal amount) {
        var userId = resolveUserId();
        var amountKey = amount.toPlainString();

        // Camada 1 — Redis: bloqueia retries de rede e comandos de voz duplicados
        if (!idempotencyService.acquireLock(userId, "withdraw", amountKey)) {
            return "Saque de R$ " + fmt(amount) + " já foi processado recentemente. " +
                   "Consulte seu extrato para confirmar antes de tentar novamente.";
        }

        try {
            // Camada 2 — MySQL: SELECT FOR UPDATE garante exclusividade na linha
            var account = accountRepository.findByKeycloakUserIdWithLock(userId)
                    .orElseThrow(() -> new RuntimeException("Conta não encontrada para o usuário autenticado"));

            // Camada 3 — Domínio: valida balance >= amount (InsufficientBalanceException)
            var transaction = account.withdraw(amount);
            accountRepository.save(account);
            bankTransactionRepository.save(transaction, account.getId());

            return "Saque de R$ " + fmt(amount) + " realizado com sucesso. " +
                   "Saldo atual: R$ " + fmt(account.getBalance());

        } catch (Exception e) {
            // Libera lock para permitir nova tentativa legítima
            idempotencyService.releaseLock(userId, "withdraw", amountKey);
            throw e;
        }
    }

    // ═══════════════════════════════════════════════════════
    // TRANSFERÊNCIA
    // Camadas: Redis idempotência + MySQL Lock Pessimista ordenado + regra de domínio
    //
    // Técnica de lock ordenado por ID (solução clássica anti-deadlock):
    //   Sem ordenação: A→B e B→A simultâneos causam deadlock
    //     Thread 1: lock(A), aguarda lock(B) ←┐
    //     Thread 2: lock(B), aguarda lock(A) ←┘ deadlock!
    //   Com ordenação: sempre lockamos conta de menor ID primeiro
    //     Thread 1: lock(A), lock(B) → executa
    //     Thread 2: aguarda lock(A) → executa depois → sem deadlock ✓
    // ═══════════════════════════════════════════════════════

    @Tool(name = "transfer",
          description = "Realiza uma transferência em reais da conta do usuário autenticado para outra conta bancária")
    @Transactional
    public String transfer(
            @ToolParam(description = "Valor a transferir em reais (ex: 300.00)") BigDecimal amount,
            @ToolParam(description = "Número da conta bancária de destino (ex: 0002-1)") String targetAccountNumber) {

        var userId = resolveUserId();
        var amountKey = amount.toPlainString();

        // Camada 1 — Redis: previne transferência duplicada por retry/voz
        if (!idempotencyService.acquireLock(userId, "transfer", amountKey, targetAccountNumber)) {
            return "Transferência de R$ " + fmt(amount) + " para a conta " + targetAccountNumber +
                   " já foi processada recentemente. Consulte seu extrato para confirmar.";
        }

        try {
            // Pré-leitura sem lock para descobrir os IDs e definir a ordem de lock
            var fromPreview = accountRepository.findByKeycloakUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Conta de origem não encontrada"));
            var toPreview = accountRepository.findByAccountNumber(targetAccountNumber)
                    .orElseThrow(() -> new RuntimeException("Conta de destino '" + targetAccountNumber + "' não encontrada"));

            if (fromPreview.getId().equals(toPreview.getId())) {
                throw new RuntimeException("Não é possível transferir para a própria conta");
            }

            // Camada 2 — MySQL: adquire locks em ordem crescente de ID (anti-deadlock)
            Account fromAccount;
            Account toAccount;

            if (fromPreview.getId() < toPreview.getId()) {
                // Lock na conta de menor ID primeiro
                fromAccount = accountRepository.findByKeycloakUserIdWithLock(userId)
                        .orElseThrow(() -> new RuntimeException("Conta de origem não encontrada"));
                toAccount   = accountRepository.findByAccountNumberWithLock(targetAccountNumber)
                        .orElseThrow(() -> new RuntimeException("Conta de destino não encontrada"));
            } else {
                // Conta de destino tem ID menor — ela é bloqueada primeiro
                toAccount   = accountRepository.findByAccountNumberWithLock(targetAccountNumber)
                        .orElseThrow(() -> new RuntimeException("Conta de destino não encontrada"));
                fromAccount = accountRepository.findByKeycloakUserIdWithLock(userId)
                        .orElseThrow(() -> new RuntimeException("Conta de origem não encontrada"));
            }

            // Camada 3 — Domínio: valida saldo (InsufficientBalanceException)
            var txOut = fromAccount.transferOut(amount);
            var txIn  = toAccount.transferIn(amount);

            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);
            bankTransactionRepository.save(txOut, fromAccount.getId());
            bankTransactionRepository.save(txIn, toAccount.getId());

            return "Transferência de R$ " + fmt(amount) +
                   " da conta " + fromAccount.getAccountNumber() +
                   " para a conta " + targetAccountNumber + " realizada com sucesso.";

        } catch (Exception e) {
            idempotencyService.releaseLock(userId, "transfer", amountKey, targetAccountNumber);
            throw e;
        }
    }

    // ═══════════════════════════════════════════════════════
    // PLANEJAMENTO DE CONTAS — Listagem
    // ═══════════════════════════════════════════════════════

    @Tool(name = "get-bill-planning",
          description = "Lista todas as contas agendadas (pendentes e pagas) do usuário autenticado")
    @Transactional(readOnly = true)
    public List<BillPlanningDto> getBillPlanning() {
        var userId = resolveUserId();
        var account = accountRepository.findByKeycloakUserId(userId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada para o usuário autenticado"));
        return billPlanningMapper.toDto(billPlanningRepository.findByAccountId(account.getId()));
    }

    // ═══════════════════════════════════════════════════════
    // PLANEJAMENTO DE CONTAS — Agendamento
    // ═══════════════════════════════════════════════════════

    @Tool(name = "schedule-bill",
          description = "Agenda uma nova conta ou boleto a pagar para o usuário autenticado")
    @Transactional
    public String scheduleBill(
            @ToolParam(description = "Nome ou descrição da conta (ex: Aluguel, Internet, Luz)") String description,
            @ToolParam(description = "Valor da conta em reais (ex: 150.00)") BigDecimal amount,
            @ToolParam(description = "Data de vencimento no formato ISO yyyy-MM-dd (ex: 2026-10-15)") String dueDate) {

        var userId = resolveUserId();
        var account = accountRepository.findByKeycloakUserId(userId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada para o usuário autenticado"));

        var due = LocalDate.parse(dueDate);
        var bill = new BillPlanning(null, account.getId(), description, due, amount, BillPlanningStatus.PENDING);
        billPlanningRepository.save(bill);

        return "Conta '" + description + "' de R$ " + fmt(amount) +
               " agendada para " + due + " com status PENDENTE.";
    }

    // ═══════════════════════════════════════════════════════
    // PLANEJAMENTO DE CONTAS — Pagamento por voz
    // Camadas: Redis idempotência + MySQL Lock Pessimista + regra de domínio
    // ═══════════════════════════════════════════════════════

    @Tool(name = "pay-bill",
          description = "Paga uma conta pendente pelo nome. Debita o valor do saldo e marca a conta como PAGA")
    @Transactional
    public String payBill(
            @ToolParam(description = "Nome ou descrição da conta pendente a pagar (ex: Internet, Aluguel)") String description) {

        var userId = resolveUserId();
        var descKey = description.toLowerCase().trim();

        // Camada 1 — Redis: evita pagamento duplicado de conta
        if (!idempotencyService.acquireLock(userId, "pay-bill", descKey)) {
            return "O pagamento da conta '" + description + "' já foi processado recentemente. " +
                   "Consulte seu extrato para confirmar.";
        }

        try {
            // Camada 2 — MySQL: lock pessimista na conta bancária
            var account = accountRepository.findByKeycloakUserIdWithLock(userId)
                    .orElseThrow(() -> new RuntimeException("Conta não encontrada para o usuário autenticado"));

            var bill = billPlanningRepository
                    .findPendingByAccountIdAndDescription(account.getId(), description)
                    .orElseThrow(() -> new RuntimeException(
                            "Conta pendente '" + description + "' não encontrada para o usuário autenticado"));

            // Camada 3 — Domínio: debita saldo (InsufficientBalanceException) e marca como PAGA
            var transaction = account.withdraw(bill.getAmount());
            bill.pay();

            accountRepository.save(account);
            bankTransactionRepository.save(transaction, account.getId());
            billPlanningRepository.save(bill);

            return "Conta '" + bill.getDescription() + "' de R$ " + fmt(bill.getAmount()) +
                   " paga com sucesso. Saldo atual: R$ " + fmt(account.getBalance());

        } catch (Exception e) {
            idempotencyService.releaseLock(userId, "pay-bill", descKey);
            throw e;
        }
    }

    // ═══════════════════════════════════════════════════════
    // INSIGHTS FINANCEIROS
    // ═══════════════════════════════════════════════════════

    @Tool(name = "generate-financial-insights",
          description = """
              Retorna um relatório financeiro consolidado do usuário: salário, saldo atual,
              contas pendentes com total comprometido, histórico de transações e indicador de
              comprometimento de renda. O modelo usará esses dados para gerar um diagnóstico
              crítico e personalizado do orçamento do usuário.
              """)
    @Transactional(readOnly = true)
    public String generateFinancialInsights() {
        var userId = resolveUserId();
        var account = accountRepository.findByKeycloakUserId(userId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada para o usuário autenticado"));

        var transactions = bankTransactionRepository.findByAccountId(account.getId());
        var allBills     = billPlanningRepository.findByAccountId(account.getId());

        var pendingBills = allBills.stream()
                .filter(b -> b.getStatus() == BillPlanningStatus.PENDING)
                .toList();

        var pendingTotal = pendingBills.stream()
                .map(BillPlanning::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var availableBalance = account.getBalance().subtract(pendingTotal);

        var sb = new StringBuilder();
        sb.append("=== RELATÓRIO FINANCEIRO DO USUÁRIO ===\n\n");

        sb.append("📊 RESUMO DA CONTA:\n");
        sb.append("  Banco:                          ").append(account.getBank().getName()).append("\n");
        sb.append("  Número da Conta:                ").append(account.getAccountNumber()).append("\n");
        sb.append("  Salário Mensal:                 R$ ").append(fmt(account.getMonthlySalary())).append("\n");
        sb.append("  Saldo Atual:                    R$ ").append(fmt(account.getBalance())).append("\n");
        sb.append("  Total de Contas Pendentes:      R$ ").append(fmt(pendingTotal)).append("\n");
        sb.append("  Saldo Disponível (líquido):     R$ ").append(fmt(availableBalance)).append("\n\n");

        sb.append("📋 CONTAS AGENDADAS — ").append(pendingBills.size()).append(" pendente(s):\n");
        if (pendingBills.isEmpty()) {
            sb.append("  Nenhuma conta pendente.\n");
        } else {
            pendingBills.forEach(b -> sb.append("  • ").append(b.getDescription())
                    .append(": R$ ").append(fmt(b.getAmount()))
                    .append(" | Vence em: ").append(b.getDueDate()).append("\n"));
        }
        sb.append("\n");

        sb.append("💳 HISTÓRICO DE TRANSAÇÕES — ").append(transactions.size()).append(" registro(s):\n");
        if (transactions.isEmpty()) {
            sb.append("  Nenhuma transação registrada.\n");
        } else {
            transactions.forEach(t -> sb.append("  • ").append(t.transactionType())
                    .append(": R$ ").append(fmt(t.amount()))
                    .append(" em ").append(t.createdAt().toLocalDate()).append("\n"));
        }

        var salary = account.getMonthlySalary();
        if (salary != null && salary.compareTo(BigDecimal.ZERO) > 0) {
            var commitment = pendingTotal
                    .divide(salary, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP);
            sb.append("\n📈 INDICADORES:\n");
            sb.append("  Comprometimento do salário com contas fixas: ").append(commitment).append("%\n");
            if (commitment.compareTo(BigDecimal.valueOf(30)) > 0) {
                sb.append("  ⚠️  ATENÇÃO: Mais de 30% do salário comprometido com contas fixas.\n");
            }
        }

        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════
    // Helpers privados
    // ═══════════════════════════════════════════════════════

    /** Obtém o Keycloak User ID (sub do JWT) do contexto de segurança do Spring. */
    private String resolveUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /** Formata BigDecimal com 2 casas decimais para exibição. */
    private String fmt(BigDecimal value) {
        if (value == null) return "0.00";
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
