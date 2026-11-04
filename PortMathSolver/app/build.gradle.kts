plugins { id("com.android.application") }
android {
 namespace = "com.port.mathsolver"
 compileSdk = 35
 defaultConfig { applicationId = "com.port.mathsolver"; minSdk = 24; targetSdk = 34; versionCode = 1; versionName = "1.0.0" }
 buildTypes { release { isMinifyEnabled = false } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies {
 implementation("androidx.core:core:1.13.1")
 implementation("com.google.mlkit:text-recognition:16.0.1")
}
