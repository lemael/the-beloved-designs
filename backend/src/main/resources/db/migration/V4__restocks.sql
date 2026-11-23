SET search_path TO beloved_designs;

-- Back-office admin : ravitaillements prévus / reçus
CREATE TABLE restocks (
    id              BIGSERIAL PRIMARY KEY,
    expected_date   DATE NOT NULL,
    received_at     TIMESTAMPTZ,
    supplier        VARCHAR(150),
    status          VARCHAR(20) NOT NULL DEFAULT 'PREVU'
                    CHECK (status IN ('PREVU', 'RECU', 'ANNULE')),
    notes           TEXT,
    created_by      BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE restock_items (
    id          BIGSERIAL PRIMARY KEY,
    restock_id  BIGINT NOT NULL REFERENCES restocks(id) ON DELETE CASCADE,
    product_id  BIGINT NOT NULL REFERENCES products(id),
    variant_id  BIGINT REFERENCES product_variants(id),
    quantity    INTEGER NOT NULL CHECK (quantity > 0),
    unit_cost   NUMERIC(12,2) CHECK (unit_cost >= 0)
);

CREATE INDEX idx_restocks_status_expected_date ON restocks(status, expected_date);
CREATE INDEX idx_restock_items_restock_id ON restock_items(restock_id);

-- Bilan annuel : filtrage des commandes par date
CREATE INDEX idx_orders_created_at ON orders(created_at);
