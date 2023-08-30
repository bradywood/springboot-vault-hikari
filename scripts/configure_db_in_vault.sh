#!/bin/sh

. ../vault.env

echo '### Enabling db plugin'
vault secrets enable database
echo '### Configuring postgres'
vault write database/config/postgres \
    plugin_name=postgresql-database-plugin \
    allowed_roles="*" \
    connection_url="postgresql://{{username}}:{{password}}@localhost:5432/?sslmode=disable" \
    username="testuser" \
    password="testpassword"

echo '### Create dynamic db role'
vault write database/roles/flywayrole db_name=postgres \
    creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; GRANT flywayrole TO \"{{name}}\";" \
    revocation_statements="DROP ROLE \"{{name}}\";" default_ttl="2m" max_ttl="2m"

vault read database/roles/flywayrole