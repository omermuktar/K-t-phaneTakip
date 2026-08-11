CREATE TABLE books (
    id BIGSERIAL PRIMARY KEY,
    isbn VARCHAR(20) NOT NULL,
    title VARCHAR(180) NOT NULL,
    author VARCHAR(120) NOT NULL,
    category VARCHAR(80) NOT NULL,
    total_copies INTEGER NOT NULL CHECK (total_copies > 0),
    available_copies INTEGER NOT NULL CHECK (available_copies >= 0 AND available_copies <= total_copies),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_books_isbn UNIQUE (isbn)
);

CREATE TABLE members (
    id BIGSERIAL PRIMARY KEY,
    membership_number VARCHAR(30) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_members_number UNIQUE (membership_number),
    CONSTRAINT uq_members_email UNIQUE (email)
);

CREATE TABLE loans (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL REFERENCES books(id),
    member_id BIGINT NOT NULL REFERENCES members(id),
    borrowed_at TIMESTAMPTZ NOT NULL,
    due_at TIMESTAMPTZ NOT NULL,
    returned_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'RETURNED')),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_loans_status_due_at ON loans(status, due_at);
CREATE INDEX idx_loans_member ON loans(member_id);
CREATE INDEX idx_loans_book ON loans(book_id);
CREATE UNIQUE INDEX uq_active_loan_per_member_book
    ON loans(member_id, book_id)
    WHERE status = 'ACTIVE';
