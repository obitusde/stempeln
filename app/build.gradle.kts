// Stempeln-App: Anzeige, Knoepfe, Planen, Urlaub, Waechter (Wecker + Ortszonen). Stempeln selbst macht MacroDroid.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.obitusde.stempeln"
    compileSdk = 35
    defaultConfig {
        applicationId = "io.github.obitusde.stempeln"
        minSdk = 33
        targetSdk = 35
        versionCode = 3
        versionName = "0.2.1"
    }
    // Fester Schluessel aus den GitHub-Secrets (STEMPELN_KEYSTORE als Base64, STEMPELN_PASSWORT), damit sich die App
    // selbst aktualisieren kann. Ohne Secrets: Android-Debug-Schluessel (dann muss man fuer ein Update neu installieren).
    val ks = System.getenv("STEMPELN_KEYSTORE_DATEI")
    signingConfigs {
        if (ks != null && file(ks).exists()) create("fest") {
            storeFile = file(ks); storePassword = System.getenv("STEMPELN_PASSWORT")
            keyAlias = "stempeln"; keyPassword = System.getenv("STEMPELN_PASSWORT")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("fest") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
    implementation(project(":logik"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
}
