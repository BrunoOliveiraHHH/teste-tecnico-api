# API de Tabela Tarifária de Água

API REST para gerenciar e calcular tarifas de água por **categoria de
consumidor** e **faixas progressivas de consumo**. O sistema é **parametrizável**:
faixas e valores ficam no banco de dados, e ajustes refletem nos cálculos **sem
alteração de código**.

## Stack

- **Java 21** · **Spring Boot 4** · **PostgreSQL 18**
- Spring Data JPA · Flyway (migrations) · Bean Validation · Maven

## Pré-requisitos

- **Java 21+** (o projeto inclui o Maven Wrapper — use `./mvnw` ou `mvnw.cmd`)
- **PostgreSQL 18** (local ou via Docker)

## Configuração do banco de dados

A aplicação lê a conexão de variáveis de ambiente (com valores padrão):

| Variável | Padrão |
|----------|--------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/tarifa_agua` |
| `DB_USER` | `tarifa` |
| `DB_PASSWORD` | `tarifa` |

O **schema é criado automaticamente pelo Flyway** na inicialização.

Subindo o PostgreSQL com Docker:

```bash
docker compose -f docker-compose-postgresql.yml up -d
```

> Sem Docker: crie o banco e o usuário no seu PostgreSQL local
> (`CREATE ROLE tarifa LOGIN PASSWORD 'tarifa';` e
> `CREATE DATABASE tarifa_agua OWNER tarifa;`).

## Como executar

```bash
docker compose -f docker-compose-postgresql.yml up -d   # 1. banco
mvn spring-boot:run                                     # 2. aplicação
```

A API fica em `http://localhost:8080`. Na primeira inicialização, o Flyway cria
o schema e insere uma tabela de exemplo (seed) que já reproduz o caso
INDUSTRIAL 18 m³ = R$ 26,00.

## Regras de negócio

**Validação das faixas (na criação):** a primeira faixa inicia em 0; `inicio <
fim`; sem sobreposição; sem lacunas (cada faixa começa logo após a anterior).

**Cálculo progressivo:** o consumo em cada faixa é multiplicado pelo valor
unitário daquela faixa, e os subtotais são somados.

| Faixa | m³ cobrados | Valor unitário | Subtotal |
|-------|-------------|----------------|----------|
| 0–10 | 10 | R$ 1,00 | R$ 10,00 |
| 11–20 | 8 | R$ 2,00 | R$ 16,00 |
| **Total (18 m³)** | | | **R$ 26,00** |

## Endpoints

### 1. Criar tabela tarifária — `POST /api/tabelas-tarifarias`

**Request:**

```json
{
  "nome": "Tabela Tarifária 2026",
  "dataVigencia": "2026-01-01",
  "categorias": [
    {
      "categoria": "INDUSTRIAL",
      "faixas": [
        { "inicio": 0,  "fim": 10,    "valorUnitario": 1.00 },
        { "inicio": 11, "fim": 20,    "valorUnitario": 2.00 },
        { "inicio": 21, "fim": 30,    "valorUnitario": 3.00 },
        { "inicio": 31, "fim": 99999, "valorUnitario": 4.00 }
      ]
    }
  ]
}
```

**Response `201 Created`:** a tabela criada, com `id`, `ativo` e as faixas (com
seus `id`), agrupadas por categoria.

### 2. Listar tabelas — `GET /api/tabelas-tarifarias`

**Response `200 OK`:** lista das tabelas ativas com suas categorias e faixas.

### 3. Excluir tabela — `DELETE /api/tabelas-tarifarias/{id}`

**Response `204 No Content`.** Exclusão **lógica**: a tabela deixa de aparecer na
listagem e não é mais usada em cálculos. Id inexistente → `404 Not Found`.

### 4. Calcular valor — `POST /api/calculos`

**Request:**

```json
{ "categoria": "INDUSTRIAL", "consumo": 18 }
```

**Response `200 OK`:**

```json
{
  "categoria": "INDUSTRIAL",
  "consumoTotal": 18,
  "valorTotal": 26.00,
  "detalhamento": [
    { "faixa": { "inicio": 0, "fim": 10 }, "m3Cobrados": 10, "valorUnitario": 1.00, "subtotal": 10.00 },
    { "faixa": { "inicio": 11, "fim": 20 }, "m3Cobrados": 8, "valorUnitario": 2.00, "subtotal": 16.00 }
  ]
}
```

### Respostas de erro

Formato padronizado (`timestamp`, `status`, `erro`, `mensagem`, `detalhes`):

| Situação | Status |
|----------|--------|
| Payload inválido / validação | `400 Bad Request` |
| Tabela não encontrada | `404 Not Found` |
| Faixas inconsistentes / consumo fora da cobertura | `422 Unprocessable Content` |

## Demonstração da parametrização

1. Calcule `INDUSTRIAL` / `18` → **R$ 26,00**.
2. Altere no banco o valor da faixa `0–10`:
   ```sql
   UPDATE faixa_consumo SET valor_unitario = 2.00
   WHERE categoria = 'INDUSTRIAL' AND inicio = 0;
   ```
3. Calcule de novo → **R$ 36,00** (10×2,00 + 8×2,00). Sem alterar código.

## Como testar

```bash
# Testes unitários (cálculo e validações) — JUnit + Mockito
mvn test
```

Exemplos de uso da API podem ser executados com `curl` (ver seção Endpoints) ou
qualquer cliente HTTP.

## Scripts de banco de dados

Versionados pelo **Flyway**, em `src/main/resources/db/migration`:

- `V1__create_schema.sql` — criação das tabelas, constraints e índices.
- `V2__seed_dados_exemplo.sql` — dados de exemplo (seed) com as quatro categorias.

As migrations são aplicadas automaticamente na inicialização.
