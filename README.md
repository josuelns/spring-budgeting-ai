# VoiceBank — AI Personal Finance Management

> Projeto final do módulo Spring AI da trilha DIO Spring Boot.  
> Transforma uma API de orçamento por voz em um **assistente bancário e consultor financeiro inteligente**, com arquitetura limpa e integração completa de IA generativa.

---

## Visão Geral

O VoiceBank processa comandos de voz do usuário, executa operações bancárias reais no banco de dados e retorna respostas em áudio — tudo protegido por autenticação JWT via Keycloak.

```
Áudio (voz do usuário)
    │
    ▼ Whisper (Speech-to-Text)
Texto transcrito
    │
    ▼ GPT-4o-mini (Tool Calling)
Ferramenta @Tool selecionada
    │
    ▼ Use case (MySQL + Redis)
Resultado da operação
    │
    ▼ TTS (Text-to-Speech)
Áudio de resposta (MP3)
```

---

## Funcionalidades — Ferramentas de IA (@Tool)

| Ferramenta             | Comando de voz (exemplo)                        | Descrição                                                   |
|------------------------|-------------------------------------------------|-------------------------------------------------------------|
| `get-statement`        | "Qual é o meu extrato?"                         | Retorna o histórico de transações da conta                  |
| `deposit`              | "Depositar duzentos reais"                      | Credita valor com @Retry para tolerância a falhas           |
| `withdraw`             | "Sacar cem reais"                               | Debita com Lock Pessimista (anti double-spending)           |
| `transfer`             | "Transferir trezentos para a conta 0002-1"      | Transferência entre contas com Lock Pessimista              |
| `get-bill-planning`    | "Quais são minhas contas do mês?"               | Lista contas fixas agendadas (pendentes e pagas)            |
| `schedule-bill`        | "Agendar aluguel de mil e duzentos reais"       | Cadastra nova conta a pagar                                 |
| `pay-bill`             | "Pagar a conta da Internet"                     | Paga conta pendente e debita saldo automaticamente          |
| `generate-financial-insights` | "Como está minha saúde financeira?"    | Diagnóstico financeiro completo + dicas de economia         |

---

## Arquitetura — DDD / Clean Architecture

```
src/main/java/dio/budgeting/
├── domain/                          ← Regras de negócio puras (sem framework)
│   ├── Account.java                 ← Agregado raiz: deposit, withdraw, transfer
│   ├── BillPlanning.java            ← Conta fixa: pay()
│   ├── BankTransaction.java         ← Value object (record imutável)
│   ├── Bank.java
│   ├── BankTransactionType.java     ← DEPOSIT | WITHDRAWAL | TRANSFER_IN | TRANSFER_OUT
│   ├── BillPlanningStatus.java      ← PENDING | PAID
│   ├── InsufficientBalanceException.java
│   ├── AccountRepository.java       ← Port (interface)
│   ├── BankTransactionRepository.java
│   └── BillPlanningRepository.java
│
├── application/                     ← Casos de uso + ferramentas da IA
│   ├── BankOperationsUseCase.java   ← 8 @Tools expostos ao Spring AI
│   └── output/
│       ├── TransactionDto.java
│       └── BillPlanningDto.java
│
└── infrastructure/                  ← Adaptadores de framework
    ├── config/
    │   └── AsyncConfig.java         ← Virtual Threads (Project Loom)
    ├── http/
    │   ├── SecurityConfig.java      ← JWT Resource Server (Keycloak)
    │   └── TransactionController.java
    ├── mapper/
    │   ├── TransactionMapper.java   ← MapStruct
    │   └── BillPlanningMapper.java
    └── persistence/
        ├── entity/                  ← Entidades JPA (Bank, Account, BankTransaction, BillPlanning)
        └── repository/              ← Spring Data JPA + implementações dos ports
```

---

## Stack Tecnológica

| Tecnologia             | Versão       | Uso                                          |
|------------------------|--------------|----------------------------------------------|
| Java                   | 25           | Virtual Threads (Project Loom)               |
| Spring Boot            | 4.0.5        | Framework principal                          |
| Spring AI              | 2.0.0-M4     | Tool Calling, Speech-to-Text, Text-to-Speech |
| OpenAI GPT-4o-mini     | —            | Seleção de ferramentas e geração de resposta |
| OpenAI Whisper-1       | —            | Transcrição de áudio (Speech-to-Text)        |
| OpenAI TTS             | gpt-4o-mini-tts | Síntese de voz (Text-to-Speech)          |
| MySQL 8.0              | —            | Banco de dados relacional                    |
| Redis 7                | —            | Idempotência e cache de sessão               |
| Keycloak 24            | —            | Autenticação JWT (OAuth2 Resource Server)    |
| MapStruct              | 1.6.3        | Mapeamento Domain ↔ DTO                      |
| Resilience4j           | 2.3.0        | @Retry com backoff exponencial               |
| Lombok                 | —            | Redução de boilerplate                       |
| Docker Compose         | —            | Ambiente local (MySQL + Redis + Keycloak)    |

---

## Ambiente Local — Docker

```bash
# Inicia MySQL 8, Redis 7 e Keycloak 24 em rede
docker compose up -d
```

Serviços provisionados pelo `compose.yml`:

| Serviço   | Porta | Credenciais          |
|-----------|-------|----------------------|
| MySQL 8.0 | 3306  | root / root          |
| Redis 7   | 6379  | —                    |
| Keycloak  | 8080  | admin / admin        |

O script `src/main/resources/db/init.sql` é executado automaticamente e cria:
- Tabelas: `banks`, `accounts`, `bank_transactions`, `bill_planning`
- **Conta de teste** vinculada ao Keycloak ID `user-test-uuid-123`:
  - Saldo: R$ 1.500,00 | Salário: R$ 5.000,00
  - Contas pendentes: Internet (R$ 120,00) e Aluguel (R$ 1.200,00)

---

## Como Executar

```bash
# 1. Inicie a infraestrutura
docker compose up -d

# 2. Exporte sua chave da OpenAI
export OPENAI_API_KEY="sk-..."

# 3. Execute a aplicação
./gradlew bootRun

# 4. Envie um áudio (ex: Postman ou curl)
curl -X POST http://localhost:8080/transactions/ai \
  -H "Authorization: Bearer <JWT_KEYCLOAK>" \
  -F "file=@recording.m4a" \
  --output resposta.mp3
```

---

## Configuração — `application.yml` (resumo)

```yaml
spring:
  threads:
    virtual:
      enabled: true          # Virtual Threads em todas as requisições HTTP

  datasource:
    url: jdbc:mysql://localhost:3306/bank_assistant_db

  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/bank-assistant

resilience4j:
  retry:
    instances:
      depositRetry:
        max-attempts: 3
        enable-exponential-backoff: true
```

---

## Segurança

Todos os endpoints exigem JWT válido emitido pelo Keycloak.  
O `sub` do token é usado automaticamente pelo use case para identificar o usuário — nenhum `@Tool` aceita parâmetro de conta de origem.

```
SecurityContextHolder.getContext().getAuthentication().getName()
→ "user-test-uuid-123"  (Keycloak User ID)
→ Conta bancária correspondente no MySQL
```

---

## Referências

- [Spring AI Reference](https://docs.spring.io/spring-ai/reference/index.html)
- [Spring AI — Tools API](https://docs.spring.io/spring-ai/reference/api/tools.html)
- [Spring AI — Audio Transcriptions](https://docs.spring.io/spring-ai/reference/api/audio/transcriptions.html)
- [Spring AI — Audio Speech](https://docs.spring.io/spring-ai/reference/api/audio/speech.html)
- [DDD Layers — Trilha DIO](../README.md#ddd-layered-architecture)
- [Repository Pattern — Trilha DIO](../README.md#repository-pattern)
- [Docker Compose Support — Trilha DIO](../README.md#docker-compose-support-in-development)
