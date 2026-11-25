@echo off
REM Levanta los 3 microservicios sin instalar Maven (ver maven.ps1).
REM Ejemplos: iniciar-microservicios.bat  /  iniciar-microservicios.bat ms-usuarios  /  iniciar-microservicios.bat -Comando test
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0maven.ps1" %*
if errorlevel 1 pause
