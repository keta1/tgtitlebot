import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.10"
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "me.ketal.tgtitlebot"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.telegram:telegrambots:5.5.0")
    implementation("org.telegram:telegrambots-abilities:5.5.0")
    // implementation("org.slf4j:slf4j-simple:1.7.30")
}

java {
    targetCompatibility = JavaVersion.VERSION_11
    sourceCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = listOf("-Xno-param-assertions")
        // Don't generate not-null assertions on parameters of methods accessible from Java
    }
}

tasks.withType<ShadowJar> {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }

    tasks.getByName("build").dependsOn(this)
}