plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    //id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")

}

android {
    namespace = "com.example.unichat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.unichat"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.firestore.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    // Firebase Authentication
    implementation(libs.firebase.auth)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.play.services.auth)

    // Jetpack Compose and Material 3 dependencies
    implementation(libs.material3)
    implementation(libs.androidx.activity.compose.v172)

    dependencies {
        val room_version = "2.6.1"

        implementation(libs.androidx.room.runtime)

        // If this project uses any Kotlin source, use Kotlin Symbol Processing (KSP)
        // See Add the KSP plugin to your project
        ksp(libs.androidx.room.room.compiler)

        // If this project only uses Java source, use the Java annotationProcessor
        // No additional plugins are necessary
        annotationProcessor(libs.androidx.room.room.compiler)

        // optional - Kotlin Extensions and Coroutines support for Room
        implementation(libs.androidx.room.ktx)

        // optional - RxJava2 support for Room
        implementation(libs.androidx.room.rxjava2)

        // optional - RxJava3 support for Room
        implementation(libs.androidx.room.rxjava3)

        // optional - Guava support for Room, including Optional and ListenableFuture
        implementation(libs.androidx.room.guava)

        // optional - Test helpers
        testImplementation(libs.androidx.room.testing)

        // optional - Paging 3 Integration
        implementation(libs.androidx.room.paging)
        implementation ("com.google.firebase:firebase-firestore:25.1.1")
    }



}