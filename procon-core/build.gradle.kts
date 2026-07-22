plugins {
  alias(libs.plugins.android.library)
}

android {
  namespace = "com.proconkit.sdk"
  compileSdk = 36

  defaultConfig {
    minSdk = 26
    consumerProguardFiles("consumer-rules.pro")
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.hidden.api.bypass)
  testImplementation(libs.junit)
}
