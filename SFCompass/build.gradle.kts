plugins {
    java
    id("com.gradleup.shadow") version "9.0.0"
}

group = "dev.sfcompass"
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
    compileOnly(fileTree("../SFCore/build/libs") { include("SFCore-*.jar") })
    compileOnly(fileTree("../SFCharacter/build/libs") { include("SFCharacter-*.jar") })
}

tasks {
    shadowJar {
        archiveClassifier.set("")
    }
    build {
        dependsOn(shadowJar)
    }
    compileJava {
        options.encoding = "UTF-8"
    }
}
