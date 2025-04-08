val kotlinVersion = "1.7.20"

plugins {
    id("com.android.application") version "8.8.0" apply false
    id("org.jetbrains.kotlin.android") version "1.7.20" apply false
    kotlin("jvm") version "1.7.20"
}


dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
}
