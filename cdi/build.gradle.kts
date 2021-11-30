
plugins {
    id("org.jetbrains.kotlin.jvm") version "1.6.0"
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib-jdk8"))

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))

    api("javax.inject:javax.inject:1")
}

tasks {
    test {
        useJUnitPlatform()
    }
}
