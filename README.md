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
o schema e insere um conjunto de dados de exemplo (várias tabelas e categorias,
com a tabela vigente "Geral 2027").

## Regras de negócio

**Validação das faixas (na criação):** a primeira faixa inicia em 0; `inicio <
fim`; sem sobreposição; sem lacunas (cada faixa começa logo após a anterior).

**Cálculo progressivo:** o consumo em cada faixa é multiplicado pelo valor
unitário daquela faixa, e os subtotais são somados.

Exemplo com a tabela vigente do seed (Geral 2027), categoria INDUSTRIAL,
consumo de **20 m³**:

| Faixa | m³ cobrados | Valor unitário | Subtotal |
|-------|-------------|----------------|----------|
| 0–10 | 10 | R$ 1,50 | R$ 15,00 |
| 11–30 | 10 | R$ 2,50 | R$ 25,00 |
| **Total (20 m³)** | | | **R$ 40,00** |

## Endpoints

### 1. Criar tabela tarifária — `POST /api/tabelas-tarifarias`

**Request:**

```json
{
  "nome": "Tabela Tarifária 2028",
  "dataVigencia": "2028-01-01",
  "categorias": [
    {
      "categoria": "INDUSTRIAL",
      "faixas": [
        { "inicio": 0,  "fim": 10,    "valorUnitario": 1.50 },
        { "inicio": 11, "fim": 30,    "valorUnitario": 2.50 },
        { "inicio": 31, "fim": 60,    "valorUnitario": 3.50 },
        { "inicio": 61, "fim": 99999, "valorUnitario": 4.50 }
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
{ "categoria": "INDUSTRIAL", "consumo": 20 }
```

**Response `200 OK`** (usando a tabela vigente do seed, Geral 2027):

```json
{
  "categoria": "INDUSTRIAL",
  "consumoTotal": 20,
  "valorTotal": 40.00,
  "detalhamento": [
    { "faixa": { "inicio": 0, "fim": 10 }, "m3Cobrados": 10, "valorUnitario": 1.50, "subtotal": 15.00 },
    { "faixa": { "inicio": 11, "fim": 30 }, "m3Cobrados": 10, "valorUnitario": 2.50, "subtotal": 25.00 }
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

1. Calcule `INDUSTRIAL` / `20` → **R$ 40,00** (tabela vigente, Geral 2027).
2. Altere no banco o valor da faixa `0–10` da tabela vigente (id 3):
   ```sql
   UPDATE faixa_consumo SET valor_unitario = 2.50
   WHERE tabela_id = 3 AND categoria = 'INDUSTRIAL' AND inicio = 0;
   ```
3. Calcule de novo → **R$ 50,00** (10×2,50 + 10×2,50). Sem alterar código.

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
- `V2__seed_dados_exemplo.sql` — seed de exemplo **massivo e variado**: 3 tabelas
  ativas com vigências diferentes (a vigente é a "Geral 2027") e 1 tabela
  histórica inativa, cada uma com as 4 categorias e várias faixas.

As migrations são aplicadas automaticamente na inicialização.
