-- « Passer la commande » v1 (décision du 2026-11-25, voir ai-memory/memory/decisions.md).
-- Uniquement des ajouts : aucune colonne ni table existante n'est supprimée ou renommée.

-- 1) Détail des montants et livraison, figés sur la commande au moment où elle est passée
--    (un changement de tarif ou d'adresse ensuite ne modifie pas une commande existante).
ALTER TABLE orders
    ADD COLUMN subtotal               NUMERIC(12,2),
    ADD COLUMN first_order_discount   NUMERIC(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN delivery_fee           NUMERIC(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN delivery_city          VARCHAR(100),
    ADD COLUMN delivery_district      VARCHAR(150),
    ADD COLUMN delivery_contact_phone VARCHAR(20);

-- Commandes déjà présentes (ex. tests / admin) : sous-total = total, sans réduction ni frais.
UPDATE orders SET subtotal = total_amount WHERE subtotal IS NULL;
ALTER TABLE orders ALTER COLUMN subtotal SET NOT NULL;

-- 2) Tarifs de livraison par ville (sous le seuil de livraison gratuite).
--    city_key : nom normalisé (minuscules, sans accents ni espaces superflus).
--    La ligne '*' est le tarif des villes absentes de la liste.
CREATE TABLE delivery_fees (
    id         BIGSERIAL PRIMARY KEY,
    city_key   VARCHAR(100)  NOT NULL UNIQUE,
    city_label VARCHAR(100)  NOT NULL,
    fee        NUMERIC(12,2) NOT NULL CHECK (fee >= 0)
);

INSERT INTO delivery_fees (city_key, city_label, fee) VALUES
    ('douala',  'Douala',         1500),
    ('yaounde', 'Yaoundé',        2000),
    ('*',       'Autres villes',  3500);
