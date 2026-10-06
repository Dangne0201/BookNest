ALTER TABLE loans
    ADD COLUMN renewed_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE loans
    ADD COLUMN renewed_by_id BIGINT;

ALTER TABLE loans
    ADD CONSTRAINT fk_loans_renewed_by
        FOREIGN KEY (renewed_by_id) REFERENCES staff_accounts (id) ON DELETE RESTRICT;

ALTER TABLE loans
    ADD CONSTRAINT ck_loans_renewal_state CHECK (
        (renewed_at IS NULL AND renewed_by_id IS NULL)
        OR (renewed_at IS NOT NULL AND renewed_by_id IS NOT NULL)
    );
