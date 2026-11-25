# Levanta los microservicios de CostoStock SIN instalar Maven.
# - Descarga Maven 3.9.9 la primera vez (queda en %USERPROFILE%\.costostock\maven).
# - Usa el Java que viene con Android Studio si no tienes JAVA_HOME configurado.
#
# Uso (desde la carpeta backend):
#   .\iniciar-microservicios.bat                 -> abre los 3 servicios
#   .\iniciar-microservicios.bat ms-usuarios     -> solo uno
#   .\iniciar-microservicios.bat -Comando test   -> corre las pruebas de los 3
param(
    [string]$Servicio = "",
    [string]$Comando = "spring-boot:run"
)
$ErrorActionPreference = "Stop"
$Version = "3.9.9"
$Base = Join-Path $env:USERPROFILE ".costostock\maven"
$MavenHome = Join-Path $Base "apache-maven-$Version"
$Mvn = Join-Path $MavenHome "bin\mvn.cmd"

# 1) Maven
if (-not (Test-Path $Mvn)) {
    Write-Host "Descargando Maven $Version (solo la primera vez)..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Force -Path $Base | Out-Null
    $zip = Join-Path $Base "maven.zip"
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $url = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$Version/apache-maven-$Version-bin.zip"
    Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
    Expand-Archive -Path $zip -DestinationPath $Base -Force
    Remove-Item $zip
}

# 2) Java 17+ (JAVA_HOME o el JBR de Android Studio)
$javaOk = $env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))
if (-not $javaOk) {
    $candidatos = @(
        (Join-Path $env:ProgramFiles "Android\Android Studio\jbr"),
        (Join-Path $env:LOCALAPPDATA "Programs\Android Studio\jbr"),
        "C:\Program Files\Android\Android Studio\jbr",
        "D:\Program Files\Android\Android Studio\jbr"
    )
    $jdk = $candidatos | Where-Object { Test-Path (Join-Path $_ "bin\java.exe") } | Select-Object -First 1
    if ($jdk) {
        $env:JAVA_HOME = $jdk
    } elseif (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        Write-Host "No encontre Java. Instala un JDK 17+ o configura JAVA_HOME con la carpeta 'jbr' de Android Studio." -ForegroundColor Red
        exit 1
    }
}
Write-Host "Java: $env:JAVA_HOME" -ForegroundColor Cyan
Write-Host "Maven: $MavenHome" -ForegroundColor Cyan

if ($Servicio) { $servicios = @($Servicio) } else { $servicios = @("ms-usuarios", "ms-productos", "ms-pedidos") }

# 3) Primera vez: descargar librerias y compilar DE A UNO en esta ventana.
#    (Si los 3 descargan al mismo tiempo, Windows bloquea los archivos: "Acceso denegado".)
$Marker = Join-Path $Base "dependencias-listas.txt"
if (-not (Test-Path $Marker) -or $Comando -ne "spring-boot:run") {
    $repo = Join-Path $env:USERPROFILE ".m2\repository"
    if (Test-Path $repo) {
        Get-ChildItem $repo -Recurse -Filter "*.lastUpdated" -ErrorAction SilentlyContinue |
            Remove-Item -Force -ErrorAction SilentlyContinue
    }
}
if (-not (Test-Path $Marker) -and $Comando -eq "spring-boot:run") {
    foreach ($s in @("ms-usuarios", "ms-productos", "ms-pedidos")) {
        Write-Host ""
        Write-Host "Preparando $s (descarga librerias y compila, solo la primera vez)..." -ForegroundColor Yellow
        Push-Location (Join-Path $PSScriptRoot $s)
        & $Mvn -B -DskipTests package
        $codigo = $LASTEXITCODE
        Pop-Location
        if ($codigo -ne 0) {
            Write-Host "Fallo la preparacion de $s. Revisa el error de arriba y mandalo." -ForegroundColor Red
            exit 1
        }
    }
    Set-Content -Path $Marker -Value (Get-Date)
}

# Las pruebas se corren de a una en esta misma ventana.
if ($Comando -ne "spring-boot:run") {
    foreach ($s in $servicios) {
        Push-Location (Join-Path $PSScriptRoot $s)
        # net.bytebuddy.experimental: Mockito compatible con Java 25 (el que trae Android Studio)
        & $Mvn -B $Comando "-DargLine=-Dnet.bytebuddy.experimental=true"
        Pop-Location
    }
    exit 0
}

# 4) Una ventana por microservicio
foreach ($s in $servicios) {
    $dir = Join-Path $PSScriptRoot $s
    if (-not (Test-Path (Join-Path $dir "pom.xml"))) {
        Write-Host "No existe el servicio '$s'" -ForegroundColor Red
        continue
    }
    $cmd = "`$host.UI.RawUI.WindowTitle = '$s'; `$env:JAVA_HOME = '$($env:JAVA_HOME)'; Set-Location '$dir'; & '$Mvn' $Comando"
    Start-Process powershell -ArgumentList "-NoExit", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $cmd
    Write-Host "Abriendo $s ($Comando)..." -ForegroundColor Green
}
Write-Host "Listo. Espera a ver 'Started Ms...Application' en cada ventana." -ForegroundColor Green
