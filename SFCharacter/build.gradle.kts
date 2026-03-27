plugins {
    java
    id("com.gradleup.shadow") version "9.0.0"
}

group = "dev.sfcharacter"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("org.sqlite", "dev.sfcharacter.libs.sqlite")
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
