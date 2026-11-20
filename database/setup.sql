-- Run through setup-db.ps1 as the database administrator.
SELECT format('CREATE ROLE %I LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION', :'app_user')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'app_user') \gexec
ALTER ROLE :"app_user" WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD :'app_password';
SELECT format('CREATE DATABASE %I ENCODING %L TEMPLATE template0', :'database_name', 'UTF8')
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = :'database_name') \gexec
