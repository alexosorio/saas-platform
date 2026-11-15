# Migrador de la base de datos (tarea única de ECS). Se construye desde database/:
#   docker build -t db-migrator database/
FROM postgres:16-alpine
COPY schema.sql /migraciones/schema.sql
COPY migrations/ /migraciones/versiones/
COPY migrar.sh /usr/local/bin/migrar.sh
ENTRYPOINT ["/usr/local/bin/migrar.sh"]
