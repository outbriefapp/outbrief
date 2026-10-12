import groovy.json.JsonSlurper

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

/**
 * The Firebase project the app is pushed through (ADR 0010 of outbrief-server): the
 * google-services.json of its Android app, named by OUTBRIEF_FIREBASE_CONFIG (scripts/build-android.sh).
 * It is not in the repository. Its values become the string resources Firebase initializes itself
 * from, as the google-services Gradle plugin would write them. Without it the build has no pushes.
 */
val firebase: Map<String, String> = System.getenv("OUTBRIEF_FIREBASE_CONFIG")
    ?.takeIf { it.isNotBlank() }
    ?.let { path ->
        @Suppress("UNCHECKED_CAST")
        val json = JsonSlurper().parse(file(path)) as Map<String, Any?>
        val project = json["project_info"] as Map<String, Any?>
        val client = (json["client"] as List<Map<String, Any?>>).first {
            val info = (it["client_info"] as Map<String, Any?>)["android_client_info"] as Map<String, Any?>
            info["package_name"] == "ai.outbrief.app"
        }
        mapOf(
            "google_app_id" to ((client["client_info"] as Map<String, Any?>)["mobilesdk_app_id"] as String),
            "gcm_defaultSenderId" to (project["project_number"] as String),
            "google_api_key" to ((client["api_key"] as List<Map<String, Any?>>).first()["current_key"] as String),
            "project_id" to (project["project_id"] as String),
        )
    }
    ?: emptyMap()

android {
    namespace = "ai.outbrief.callservice"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
        for ((name, value) in firebase) resValue("string", name, value)
    }

    buildFeatures {
        resValues = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    // 33.x: its libraries still build against the Kotlin 1.9 of Tauri's generated project.
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation(project(":tauri-android"))
}
