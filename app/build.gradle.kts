plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.demo.google.monetization"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.demo.google.monetization"
        minSdk = 25
        targetSdk = 36
        versionCode = 1_00_006
        versionName = "1.00.006"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        setProperty("archivesBaseName", "$applicationId-v$versionName")
    }

    flavorDimensions("environment")
    productFlavors {
        create("prod") {
            dimension = "environment"
        }

        create("demo") {
            dimension = "environment"
            versionNameSuffix = "-preprod"
        }
    }

    signingConfigs {
        register("release") {
            // add a gradle.properties file at ~/.gradle with following content
            /*
                HOWARD_STORE_PASSWORD=240959747
                HOWARD_KEY_ALIAS=key0
                HOWARD_KEY_ALIAS_PASSWORD=240959747
             */
            storeFile = file("google-monetization.jks")
            storePassword = project.findProperty("HOWARD_STORE_PASSWORD") as String?
            keyAlias = project.findProperty("HOWARD_KEY_ALIAS") as String?
            keyPassword = project.findProperty("HOWARD_KEY_ALIAS_PASSWORD") as String?
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }

        debug {
            versionNameSuffix = ".debug"
            isDebuggable = true
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Google Play billing (in app purchase)
    implementation(libs.com.android.billingclient.ktx)
    // Google AdMod
    implementation(libs.play.services.ads)
}