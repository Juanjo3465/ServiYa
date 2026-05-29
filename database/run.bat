@echo off
echo "Init setup"

echo "Start MySQL whit Docker Compose..."
docker compose up -d


echo "Waiting for MySQL to be ready..."
timeout /t 30 > nul


rem Execute SQL init script (if exist)
if exist "create_database.sql" (

    echo " Excecute SQl init script..."
    rem docker exec -i serviya-mysql mysql -u root -p${SPRING_DB_PASSWORD_ROOT} ${SPRING_DB_NAME} < create_database.sql
    docker exec -i serviya-mysql mysql -u root -proot marketplace_services < create_database.sql
    echo " Script SQL excecuted successfully."
) else (
    echo " create_database.sql not found. Skipping SQL initialization."
)

