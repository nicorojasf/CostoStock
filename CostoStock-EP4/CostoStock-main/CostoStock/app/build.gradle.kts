import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    jacoco
}

// ---------------------------------------------------------------------------------------------
// Firma del APK de release: los datos de la llave están en keystore.properties (raíz del proyecto)
// y la llave en keystore/costostock-clientes.jks.
// ---------------------------------------------------------------------------------------------
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// Host de los microservicios (ver gradle.properties). 10.0.2.2 = el PC visto desde el emulador.
val apiHost: String = (project.findProperty("costostock.apiHost") as String?) ?: "10.0.2.2"

android {
    namespace = "cl.costoStock.myapplication"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "cl.costoStock.myapplication"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Microservicios propios (Spring Boot)
        buildConfigField("String", "USUARIOS_URL", "\"http://$apiHost:8081/\"")
        buildConfigField("String", "PRODUCTOS_URL", "\"http://$apiHost:8082/\"")
        buildConfigField("String", "PEDIDOS_URL", "\"http://$apiHost:8083/\"")
        // API externa: indicadores económicos de Chile (dólar, UF, euro, UTM)
        buildConfigField("String", "INDICADORES_URL", "\"https://mindicador.cl/\"")
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(keystoreProps.getProperty("storeFile", "keystore/costostock-clientes.jks"))
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        debug {
            // Genera el archivo .exec de JaCoCo al correr las pruebas unitarias.
            enableUnitTestCoverage = true
        }
        release {
            optimization {
                enable = true
            }
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    testCoverage {
        jacocoVersion = "0.8.13"
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)

    // Consumo de APIs REST
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.gson)
    implementation(libs.okhttp.logging)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// ---------------------------------------------------------------------------------------------
// Reporte de cobertura: ./gradlew jacocoTestReport
// Mide la LÓGICA de la app (ViewModels, repositorios, dominio, mapeos). Se excluyen las pantallas
// Compose, el tema, la base SQLite y los recursos nativos, que no son lógica de negocio.
// Reporte HTML: app/build/reports/jacoco/jacocoTestReport/html/index.html
// ---------------------------------------------------------------------------------------------
val coverageExclusions = listOf(
    "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*", "**/*Test*.*",
    "**/ui/theme/**", "**/ui/components/**", "**/ui/util/**",
    "**/*Screen*.*", "**/*Root*.*", "**/ComposableSingletons*.*",
    "**/MainActivity*.*", "**/*Application*.*", "**/AppContainer*.*", "**/AppViewModelProvider*.*",
    "**/data/local/**", "**/data/storage/**", "**/data/session/**", "**/nativeapi/**",
    "**/data/remote/ApiClient*.*", "**/data/remote/dto/**", "**/data/model/**"
)

tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Genera el reporte de cobertura de las pruebas unitarias."
    dependsOn("testDebugUnitTest")

    val buildDirFile = layout.buildDirectory.get().asFile
    classDirectories.setFrom(
        fileTree(File(buildDirFile, "intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")) {
            exclude(coverageExclusions)
        },
        fileTree(File(buildDirFile, "tmp/kotlin-classes/debug")) { exclude(coverageExclusions) }
    )
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(
        fileTree(buildDirFile) {
            include("outputs/unit_test_code_coverage/debugUnitTest/*.exec", "jacoco/*.exec")
        }
    )
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
