plugins {
    java
    id("com.gradleup.shadow") version "9.0.0"
}

group = "dev.sfcore"
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
    // SFCharacter NO es compileOnly — la integración se hace vía reflexión para evitar ciclo de compilación
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")
    implementation("com.mysql:mysql-connector-j:8.3.0")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        mergeServiceFiles() // sqlite-jdbc y mysql-connector traen META-INF/services/java.sql.Driver
        relocate("com.zaxxer.hikari", "dev.sfcore.libs.hikari")
        // org.sqlite NO se relocaliza: tiene JNI bindings — el .dll/.so nativo
        // busca símbolos Java en org/sqlite/core/NativeDB y relocalizar rompe el link.
        // mysql-connector NO se relocaliza: hace Class.forName con string literal internamente
        minimize {
            exclude(dependency("com.zaxxer:HikariCP:.*"))
            exclude(dependency("org.xerial:sqlite-jdbc:.*"))
            exclude(dependency("com.mysql:mysql-connector-j:.*"))
        }
    }
    build {
        dependsOn(shadowJar)
    }
    compileJava {
        options.encoding = "UTF-8"
    }
}
