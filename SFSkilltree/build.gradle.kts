plugins {
    java
    id("com.gradleup.shadow") version "9.0.0"
}

group = "dev.skilltree"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.oraxen.com/releases")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("io.th0rgal:oraxen:1.210.0")
    compileOnly(fileTree("../SFCore/build/libs") { include("SFCore-*.jar") })
    compileOnly(fileTree("../SFCrafting/build/libs") { include("SFCrafting-*.jar") })
    compileOnly(fileTree("../SFDrops/build/libs") { include("SFDrops-*.jar") })
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
