CREATE TABLE IF NOT EXISTS contact_info (
    url VARCHAR(1000) PRIMARY KEY,
    title VARCHAR(500),
    timestamp BIGINT,
    phones CLOB,
    emails CLOB,
    addresses CLOB
);

CREATE INDEX IF NOT EXISTS idx_timestamp ON contact_info(timestamp);
CREATE INDEX IF NOT EXISTS idx_title ON contact_info(title);