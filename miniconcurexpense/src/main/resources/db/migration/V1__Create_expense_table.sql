-- flyway will run this script inside every schema that is created
CREATE TABLE expense (
    id SERIAL PRIMARY KEY,
    description TEXT NOT NULL,
    amount NUMERIC(10,2) NOT NULL
);
