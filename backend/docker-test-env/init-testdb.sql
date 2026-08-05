CREATE TABLE employees (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(50) NOT NULL,
    salary NUMERIC(10, 2)
);

INSERT INTO employees (name, department, salary) VALUES
    ('Alice Smith', 'Engineering', 95000.00),
    ('Bob Jones', 'Marketing', 75000.00),
    ('Charlie Brown', 'Sales', 65000.00),
    ('Diana Prince', 'Management', 120000.00);

CREATE TABLE projects (
    id SERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    budget NUMERIC(12, 2)
);

INSERT INTO projects (title, budget) VALUES
    ('Project Heimdall', 500000.00),
    ('Project Apollo', 250000.00);
