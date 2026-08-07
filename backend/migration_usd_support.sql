-- Migración para agregar soporte de conversión de moneda en transacciones
-- Fecha: 2026-01-18
-- Descripción: Agrega campos para registrar información de conversión ARS/USD

-- Agregar columna para la moneda de la transacción
ALTER TABLE transactions 
ADD COLUMN currency VARCHAR(10) AFTER state;

-- Agregar columna para el monto original (antes de conversión)
ALTER TABLE transactions 
ADD COLUMN original_amount DOUBLE AFTER currency;

-- Agregar columna para la moneda original
ALTER TABLE transactions 
ADD COLUMN original_currency VARCHAR(10) AFTER original_amount;

-- Agregar columna para la tasa de cambio aplicada
ALTER TABLE transactions 
ADD COLUMN exchange_rate DOUBLE AFTER original_currency;

-- Agregar columna para el monto de impuestos
ALTER TABLE transactions 
ADD COLUMN tax_amount DOUBLE AFTER exchange_rate;

-- Agregar columna para el porcentaje de impuesto
ALTER TABLE transactions 
ADD COLUMN tax_percentage DOUBLE AFTER tax_amount;

-- Actualizar transacciones existentes para establecer la moneda como ARS por defecto
UPDATE transactions 
SET currency = 'ARS' 
WHERE currency IS NULL;

-- Crear índice para mejorar búsquedas por moneda
CREATE INDEX idx_transactions_currency ON transactions(currency);

-- Crear índice para mejorar búsquedas de transacciones con conversión
CREATE INDEX idx_transactions_converted ON transactions(original_currency, exchange_rate);
