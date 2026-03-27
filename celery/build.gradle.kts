plugins {
    `java-library`
    alias(libs.plugins.shadow)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.jetbrainsannotations)
    implementation(libs.mongo)
    implementation(libs.influxdb)
implementation(libs.nats)
    implementation(libs.lettuce)
    api(libs.gson)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
