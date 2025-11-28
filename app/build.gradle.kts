import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.githarshking.the_digital_munshi"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.githarshking.the_digital_munshi"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // --- DEBUGGING LOGIC START ---
        val rootFile = project.rootProject.file("local.properties")
        val appFile = project.file("local.properties")
        val properties = Properties()
        var geminiKey = ""

        println("🔍 DEBUGGING API KEY:")
        if (rootFile.exists()) {
            println("   ✅ Found local.properties in ROOT folder: ${rootFile.absolutePath}")
            properties.load(FileInputStream(rootFile))
            geminiKey = properties.getProperty("geminiApiKey") ?: ""
        } else if (appFile.exists()) {
            println("   ⚠️ Found local.properties in APP folder (Wrong spot, but reading anyway): ${appFile.absolutePath}")
            properties.load(FileInputStream(appFile))
            geminiKey = properties.getProperty("geminiApiKey") ?: ""
        } else {
            println("   ❌ ERROR: Could not find local.properties anywhere!")
        }

        if (geminiKey.isEmpty()) {
            println("   ❌ ERROR: Key 'geminiApiKey' is MISSING or EMPTY inside the file.")
        } else {
            println("   ✅ SUCCESS: Key found! Length: ${geminiKey.length}")
        }
        println("--------------------------------------------------")
        // --- DEBUGGING LOGIC END ---

        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.navigation.compose)

    // Room, Coroutines, and ViewModel using version catalog
    implementation(libs.androidx.room.runtime)
    annotationProcessor(libs.androidx.room.compiler)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.zxing.core)

    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    implementation(libs.androidx.work.runtime.ktx)
}