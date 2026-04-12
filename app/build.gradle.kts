import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// --------------- Auto-versioning ---------------
val versionPropsFile = file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) {
        versionPropsFile.inputStream().use { load(it) }
    }
}
val appVersionCode = versionProps.getProperty("versionCode", "9").toInt()
val appVersionMajor = versionProps.getProperty("versionMajor", "1").toInt()
val appVersionMinor = versionProps.getProperty("versionMinor", "3").toInt()
val appVersionPatch = versionProps.getProperty("versionPatch", "1").toInt()
val appVersionName = "$appVersionMajor.$appVersionMinor.$appVersionPatch"
// -----------------------------------------------

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

// Enables official Firebase resource generation when google-services.json is present.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "br.com.tec.tecmotors"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "br.com.tec.tecmotors"
        minSdk = 24
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

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
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
    }

    applicationVariants.all {
        val variant = this
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "TecMotors-v${variant.versionName}-build${variant.versionCode}-${variant.buildType.name}.apk"
        }
    }
}

// Auto-increment version after successful assemble
tasks.configureEach {
    if (name == "assembleDebug" || name == "assembleRelease") {
        doLast {
            val props = Properties().apply {
                versionPropsFile.inputStream().use { load(it) }
            }
            val newCode = props.getProperty("versionCode").toInt() + 1
            val newPatch = props.getProperty("versionPatch").toInt() + 1
            val major = props.getProperty("versionMajor")
            val minor = props.getProperty("versionMinor")
            props.setProperty("versionCode", newCode.toString())
            props.setProperty("versionPatch", newPatch.toString())
            versionPropsFile.writeText(
                "versionCode=$newCode\nversionMajor=$major\nversionMinor=$minor\nversionPatch=$newPatch\n"
            )
            println(">> TecMotors version bumped: versionCode=$newCode, versionName=$major.$minor.$newPatch")
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":shared"))
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.play.services.auth)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
