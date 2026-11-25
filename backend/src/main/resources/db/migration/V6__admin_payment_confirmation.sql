-- Back-office « Confirmation paiement » (décision du 2026-11-25, voir ai-memory/memory/decisions.md).
-- Uniquement des ajouts.

-- Réduction accordée à la main par l'admin (en plus de la réduction de première commande du site).
ALTER TABLE orders ADD COLUMN manual_discount NUMERIC(12,2) NOT NULL DEFAULT 0;

-- Date du paiement saisie par l'admin (le mode de paiement va dans la colonne existante « provider »).
ALTER TABLE payments ADD COLUMN paid_on DATE;
