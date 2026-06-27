-- =====================================================================
-- Dados de exemplo (seed) - massivo e variado.
--
-- 4 tabelas tarifárias (3 ativas com vigências diferentes + 1 histórica
-- inativa), cada uma com as 4 categorias e várias faixas progressivas
-- (contíguas a partir de 0). A tabela VIGENTE (mais recente) é a "Geral 2027".
-- =====================================================================

INSERT INTO tabela_tarifaria (id, nome, data_vigencia, ativo) VALUES
  (1, 'Tabela Residencial 2025',          DATE '2025-01-01', TRUE),
  (2, 'Tabela Comercial 2026',            DATE '2026-01-01', TRUE),
  (3, 'Tabela Geral 2027',                DATE '2027-01-01', TRUE),
  (4, 'Tabela Histórica 2023 (inativa)',  DATE '2023-01-01', FALSE);

-- ---------- Tabela 1: Residencial 2025 (faixas 0-10 / 11-25 / 26-50 / 51-100 / 101+) ----------
INSERT INTO faixa_consumo (tabela_id, categoria, inicio, fim, valor_unitario) VALUES
(1, 'PARTICULAR', 0,  10,    0.75), (1, 'PARTICULAR', 11, 25,    1.50), (1, 'PARTICULAR', 26, 50,    2.25), (1, 'PARTICULAR', 51, 100,   3.00), (1, 'PARTICULAR', 101, 99999, 3.75),
(1, 'COMERCIAL',  0,  10,    1.20), (1, 'COMERCIAL',  11, 25,    2.10), (1, 'COMERCIAL',  26, 50,    3.00), (1, 'COMERCIAL',  51, 100,   3.90), (1, 'COMERCIAL',  101, 99999, 4.80),
(1, 'INDUSTRIAL', 0,  10,    1.00), (1, 'INDUSTRIAL', 11, 25,    1.90), (1, 'INDUSTRIAL', 26, 50,    2.80), (1, 'INDUSTRIAL', 51, 100,   3.70), (1, 'INDUSTRIAL', 101, 99999, 4.60),
(1, 'PUBLICO',    0,  10,    0.90), (1, 'PUBLICO',    11, 25,    1.70), (1, 'PUBLICO',    26, 50,    2.50), (1, 'PUBLICO',    51, 100,   3.30), (1, 'PUBLICO',    101, 99999, 4.10);

-- ---------- Tabela 2: Comercial 2026 (faixas 0-15 / 16-40 / 41-80 / 81-150 / 151+) ----------
INSERT INTO faixa_consumo (tabela_id, categoria, inicio, fim, valor_unitario) VALUES
(2, 'PARTICULAR', 0,  15,    0.85), (2, 'PARTICULAR', 16, 40,    1.65), (2, 'PARTICULAR', 41, 80,    2.45), (2, 'PARTICULAR', 81, 150,   3.25), (2, 'PARTICULAR', 151, 99999, 4.05),
(2, 'COMERCIAL',  0,  15,    1.35), (2, 'COMERCIAL',  16, 40,    2.35), (2, 'COMERCIAL',  41, 80,    3.35), (2, 'COMERCIAL',  81, 150,   4.35), (2, 'COMERCIAL',  151, 99999, 5.35),
(2, 'INDUSTRIAL', 0,  15,    1.15), (2, 'INDUSTRIAL', 16, 40,    2.15), (2, 'INDUSTRIAL', 41, 80,    3.15), (2, 'INDUSTRIAL', 81, 150,   4.15), (2, 'INDUSTRIAL', 151, 99999, 5.15),
(2, 'PUBLICO',    0,  15,    1.05), (2, 'PUBLICO',    16, 40,    1.95), (2, 'PUBLICO',    41, 80,    2.85), (2, 'PUBLICO',    81, 150,   3.75), (2, 'PUBLICO',    151, 99999, 4.65);

-- ---------- Tabela 3: Geral 2027 (VIGENTE) (faixas 0-10 / 11-30 / 31-60 / 61+) ----------
INSERT INTO faixa_consumo (tabela_id, categoria, inicio, fim, valor_unitario) VALUES
(3, 'PARTICULAR', 0,  10,    0.80), (3, 'PARTICULAR', 11, 30,    1.60), (3, 'PARTICULAR', 31, 60,    2.40), (3, 'PARTICULAR', 61, 99999, 3.20),
(3, 'COMERCIAL',  0,  10,    1.40), (3, 'COMERCIAL',  11, 30,    2.60), (3, 'COMERCIAL',  31, 60,    3.80), (3, 'COMERCIAL',  61, 99999, 5.00),
(3, 'INDUSTRIAL', 0,  10,    1.50), (3, 'INDUSTRIAL', 11, 30,    2.50), (3, 'INDUSTRIAL', 31, 60,    3.50), (3, 'INDUSTRIAL', 61, 99999, 4.50),
(3, 'PUBLICO',    0,  10,    1.10), (3, 'PUBLICO',    11, 30,    2.20), (3, 'PUBLICO',    31, 60,    3.30), (3, 'PUBLICO',    61, 99999, 4.40);

-- ---------- Tabela 4: Histórica 2023 (INATIVA) (faixas 0-20 / 21-50 / 51+) ----------
INSERT INTO faixa_consumo (tabela_id, categoria, inicio, fim, valor_unitario) VALUES
(4, 'PARTICULAR', 0,  20,    0.60), (4, 'PARTICULAR', 21, 50,    1.20), (4, 'PARTICULAR', 51, 99999, 1.80),
(4, 'COMERCIAL',  0,  20,    1.00), (4, 'COMERCIAL',  21, 50,    1.90), (4, 'COMERCIAL',  51, 99999, 2.80),
(4, 'INDUSTRIAL', 0,  20,    0.90), (4, 'INDUSTRIAL', 21, 50,    1.70), (4, 'INDUSTRIAL', 51, 99999, 2.50),
(4, 'PUBLICO',    0,  20,    0.80), (4, 'PUBLICO',    21, 50,    1.50), (4, 'PUBLICO',    51, 99999, 2.20);

-- Mantém a sequence/identity após os ids inseridos manualmente.
SELECT setval(pg_get_serial_sequence('tabela_tarifaria', 'id'), (SELECT MAX(id) FROM tabela_tarifaria));
