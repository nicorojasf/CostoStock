@echo off
REM Abre una ventana por microservicio (requiere Maven en el PATH).
REM Para usar MySQL agrega: -Dspring-boot.run.profiles=mysql
cd /d %~dp0
start "ms-usuarios (8081)"  cmd /k "cd ms-usuarios && mvn spring-boot:run"
start "ms-productos (8082)" cmd /k "cd ms-productos && mvn spring-boot:run"
start "ms-pedidos (8083)"   cmd /k "cd ms-pedidos && mvn spring-boot:run"
