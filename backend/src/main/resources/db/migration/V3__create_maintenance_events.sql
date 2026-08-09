CREATE TABLE maintenance_events (
    id uuid PRIMARY KEY,
    vehicle_id uuid NOT NULL,
    service_date date NOT NULL,
    description varchar(500) NOT NULL,
    mileage integer,
    cost numeric(12, 2),
    notes varchar(2000),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT fk_maintenance_events_vehicle
        FOREIGN KEY (vehicle_id)
        REFERENCES vehicles (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_maintenance_events_mileage_nonnegative
        CHECK (mileage IS NULL OR mileage >= 0),
    CONSTRAINT ck_maintenance_events_cost_nonnegative
        CHECK (cost IS NULL OR cost >= 0)
);

CREATE INDEX ix_maintenance_events_vehicle_service_date_created_at
    ON maintenance_events (vehicle_id, service_date DESC, created_at DESC);
