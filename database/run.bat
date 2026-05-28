@echo off
echo "Init setup"

echo "Start MySQL whit Docker Compose..."
docker compose up -d


echo "Waiting for MySQL to be ready..."
timeout /t 60 > nul


rem Execute SQL init script (if exist)
if exist "create_database.sql" (

    echo " Excecute SQl init script..."
    docker exec -i serviya-mysql mysql -u root -p${DB_PASSWORD_ROOT} ${DB_NAME} < ./create_database.sql

    echo " Script SQL excecuted successfully."
) else (
    echo " create_database.sql not found. Skipping SQL initialization."
)

