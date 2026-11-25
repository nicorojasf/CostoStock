#!/usr/bin/env bash
# Levanta los tres microservicios en segundo plano (requiere Maven). Ctrl+C los detiene.
# Para usar MySQL: PERFIL=mysql ./iniciar-microservicios.sh
cd "$(dirname "$0")"
ARGS=""
if [ -n "$PERFIL" ]; then ARGS="-Dspring-boot.run.profiles=$PERFIL"; fi
trap 'kill 0' EXIT
(cd ms-usuarios  && mvn -q spring-boot:run $ARGS) &
(cd ms-productos && mvn -q spring-boot:run $ARGS) &
(cd ms-pedidos   && mvn -q spring-boot:run $ARGS) &
wait
