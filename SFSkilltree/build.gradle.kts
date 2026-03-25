plugins {
    java
    id("com.gradleup.shadow") version "9.0.0"
}

group = "dev.skilltree"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.oraxen.com/releases")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("io.th0rgal:oraxen:1.210.0")
    compileOnly(files("../../Server/plugins/SFCore-1.0.0.jar"))
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        minimize {
            exclude(dependency("org.xerial:sqlite-jdbc:.*"))
        }
    }
    build {
        dependsOn(shadowJar)
    }
    compileJava {
        options.encoding = "UTF-8"
    }
}