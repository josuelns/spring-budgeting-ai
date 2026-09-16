# spring-budgeting-ai

API financeira com **Spring Boot** e **Spring AI**: registre despesas e receitas por voz (ou JSON), com resposta em áudio, persistência em MySQL e autenticação OIDC via Keycloak.

Parte da minha transição **Node.js → Java/Spring Boot**. Portfólio completo: [josuelns.github.io](https://josuelns.github.io/)

## Stack

| Camada | Tecnologias |
|---|---|
| Runtime | Java 25, Spring Boot 4, Virtual Threads |
| IA | Spring AI, OpenAI (GPT-4o-mini, Whisper, TTS) |
| API | REST, MapStruct, Resilience4j |
| Segurança | Spring Security, OAuth2 Resource Server, Keycloak |
| Dados | Spring Data JPA, MySQL 8, Redis |
| Infra | Docker Compose |

## Destaques

- **Tool calling**: GPT persiste transações via use cases do domínio
- **Entrada por voz**: áudio → transcrição → IA → resposta em MP3
- **Auth OIDC**: JWT do Keycloak (realm `bank-assistant`)
- **Resiliência**: retry com backoff exponencial em operações de persistência
- **Cache / idempotência**: Redis para operações concorrentes

## Arquitetura

```
domain/          → entidades e regras de negócio
application/     → use cases (PersistTransaction, ListByCategory, BankOperations)
infrastructure/  → HTTP, JPA, Redis, mappers MapStruct, SecurityConfig
```

## Pré-requisitos

- Java 25+
- Docker e Docker Compose
- Variável `OPENAI_API_KEY` configurada

## Como rodar

```bash
# 1. Infra (MySQL, Redis, Keycloak) — sobe automaticamente com bootRun
docker compose up -d

# 2. API (porta padrão 8080; se Keycloak já usar 8080, use SERVER_PORT=8081)
export OPENAI_API_KEY=sua-chave
./gradlew bootRun
```

| Serviço | URL |
|---|---|
| Keycloak Admin | http://localhost:8080 (admin / admin) |
| MySQL | `localhost:3306` — db `bank_assistant_db` |
| Redis | `localhost:6379` |

> Configure o realm `bank-assistant` no Keycloak antes de chamar endpoints protegidos.

## Endpoints REST

| Método | Rota | Descrição | Auth |
|---|---|---|---|
| `POST` | `/transactions` | Cria transação (JSON) | JWT |
| `GET` | `/transactions/{category}` | Lista por categoria | JWT |
| `POST` | `/transactions/ai` | Transcrição + IA + áudio MP3 | JWT |

## Testes

```bash
./gradlew test
```

Inclui testes de integração com modelos OpenAI (requer `OPENAI_API_KEY`).

## Projetos relacionados

- [auth-api-prisma](https://github.com/josuelns/auth-api-prisma) — autenticação JWT documentada com Swagger (Node.js)
- [cloud-parking-spring](https://github.com/josuelns/cloud-parking-spring) — outra API Spring Boot com Docker

---

[Portfólio](https://josuelns.github.io/) · [GitHub](https://github.com/josuelns) · [LinkedIn](https://www.linkedin.com/in/josue-leandro-navarro)
