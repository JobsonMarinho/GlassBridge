plugins {
    kotlin("jvm") version "2.1.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "net.hypedmc.glassbridge"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("org.github.paperspigot:paperspigot-api:1.8.8-R0.1-SNAPSHOT")
    compileOnly("com.github.azbh111:craftbukkit-1.8.8:R")

    implementation(files("libs/acf-paper.jar"))
}

tasks {
    shadowJar {
        relocate("co.aikar.commands", "net.hypedmc.glassbridge.acf")
        relocate("co.aikar.locales", "net.hypedmc.glassbridge.locales")
    }
    build {
        dependsOn(shadowJar)
    }
}

kotlin {
    jvmToolchain(17)
}