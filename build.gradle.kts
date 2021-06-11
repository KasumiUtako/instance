import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar


plugins {
    val kotlinVersion = "1.4.30"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("com.github.johnrengelman.shadow") version "6.1.0"
//    id("net.mamoe.mirai-console") version "2.6.5"
}

val kotlinx_coroutines_version: String by project

group = "org.kslab"
version = "0.0.1-dev"

repositories {
    mavenLocal()
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
    mavenLocal()
}

dependencies {
    shadow(localGroovy())
    shadow(gradleApi())

    implementation("net.mamoe:mirai-console-terminal:2.6.5") // 自行替换版本
    implementation("net.mamoe:mirai-console:2.6.5") // 自行替换版本
    implementation("net.mamoe:mirai-core:2.6.5")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.4.30")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinx_coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinx_coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:$kotlinx_coroutines_version")
    implementation("com.alibaba:fastjson:1.2.75")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

kotlin {
    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
        }
    }
}


tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    named<ShadowJar>("shadowJar") {
        enabled = true
        manifest {
            archiveBaseName.set("shadow")
            attributes(
                mapOf(
                    "Main-Class" to "org.kslab.console.RunMiraiKt"
                )
            )
        }
    }
    withType<Jar> {
        manifest {
            archiveBaseName.set("shadow")
            attributes(
                mapOf(
                    "Main-Class" to "org.kslab.console.RunMiraiKt"
                )
            )
        }
    }



//    build {
//        dependsOn(shadowJar)
//    }
}