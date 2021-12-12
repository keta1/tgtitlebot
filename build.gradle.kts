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