plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.svvaap.bookhive"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.svvaap.bookhive"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.2"

        // Cloudinary config injected from gradle.properties (leave empty to disable until configured)
        buildConfigField(
                "String",
                "CLOUDINARY_CLOUD_NAME",
                "\"${project.findProperty("CLOUDINARY_CLOUD_NAME") ?: ""}\""
        )
        buildConfigField(
                "String",
                "CLOUDINARY_UNSIGNED_PRESET",
                "\"${project.findProperty("CLOUDINARY_UNSIGNED_PRESET") ?: ""}\""
        )

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

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.annotation)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)
    implementation(libs.google.signin)

    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Razorpay Checkout SDK
    implementation("com.razorpay:checkout:1.6.33")

    // HTTP client for Cloudinary uploads
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Ensure Activity Result APIs available
    implementation("androidx.activity:activity:1.9.1")
    implementation("androidx.fragment:fragment:1.6.2")


    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
