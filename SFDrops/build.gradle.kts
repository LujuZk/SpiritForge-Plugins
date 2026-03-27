plugins {
    java
    id("com.gradleup.shadow") version "9.0.0"
}

group = "dev.sfdrops"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly(files("../../../plugins/Oraxen-Plugin-1.21.11.jar"))
    compileOnly(fileTree("../SFCore/build/libs") { include("SFCore-*.jar") })
    compileOnly(fileTree("../SFSkilltree/build/libs") { include("*.jar") })
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
        options.release.set(21)
    }
}