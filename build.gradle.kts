import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        maven {
            name = "KotDis"
            url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
        }
    }
}

plugins {
    application
    `maven-publish`

    kotlin("jvm") version "1.4.10"

    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("io.gitlab.arturbosch.detekt") version "1.13.1"
}

group = "community.fabricmc.bot"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()

    maven {
        name = "KotDis (Public)"
        url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
    }

    maven {
        name = "FabricMC"
        url = uri("https://maven.fabricmc.net/")
    }

    maven {
        name = "Bintray (Linkie)"
        url = uri("https://dl.bintray.com/shedaniel/linkie")
    }

    maven {
        name = "JitPack"
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("com.kotlindiscord.kord.extensions:kord-extensions:1.4.0-RC6")
    implementation("com.kotlindiscord.kordex.ext.mappings:ext-mappings:1.1.0-RC3")
    implementation("com.uchuhimo:konf:0.23.0")
    implementation("com.uchuhimo:konf-toml:0.23.0")
    implementation("io.github.microutils:kotlin-logging:2.0.3")
    implementation("org.codehaus.groovy:groovy:3.0.4")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"

    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}

application {
    mainClassName = "community.fabricmc.bot.BotKt"
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "community.fabricmc.bot.BotKt"
        )
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

detekt {
    buildUponDefaultConfig = true
    config = rootProject.files("detekt.yml")
}
