# SpiritForge Plugins

Suite de plugins para servidor Minecraft Paper 1.21.1 con sistema RPG completo.

## Plugins

| Plugin | Descripción | Dependencias |
|--------|-------------|--------------|
| **SFCore** | Sistema central de stats (STR/VIT/INT/AGI/daño/vida/etc.) + API para otros plugins | Paper |
| **SFCompass** | Navegación por islas con niveles de brújula y zonas con efectos visuales | Paper |
| **SFCrafting** | Sistema de crafteo y forja de SpiritForge (smelter, anvil, rarezas, aura) | Paper, Oraxen |
| **SFDrops** | Manejo de drops de ores/logs con rareza gaussiana conectada a SFCore/SFSkilltree | Paper, Oraxen, SFCore, SkillTreePlugin |
| **SFSkilltree** | Árbol de habilidades RPG con 7 skills, XP, puntos y editor web | Paper, Oraxen, SFCore |

## Requisitos

- Java 21+ (validado con JDK 25)
- Paper 1.21.1+
- Oraxen 1.21.0+ (requerido por SFSkilltree, SFCrafting y SFDrops)
- Gradle (wrapper incluido en cada plugin)

## Estructura

```text
SpiritForge-Plugins/
├── SFCore/         # API de stats central
├── SFCompass/      # Sistema de islas y brújula
├── SFCrafting/     # Crafteo y forja de SpiritForge
├── SFDrops/        # Drops de ores/logs con rareza
└── SFSkilltree/    # Árbol de habilidades RPG
    └── editor/     # Editor web de árboles (React + Vite)
```

## Compilar

Cada plugin se compila de forma independiente desde su carpeta:

```bash
cd SFCore
./gradlew shadowJar   # → build/libs/SFCore-1.0.0.jar

cd SFCompass
./gradlew shadowJar   # → build/libs/SFCompass-1.0.0.jar

cd SFCrafting
./gradlew shadowJar   # → build/libs/SFCrafting-1.0.0.jar

cd SFDrops
./gradlew shadowJar   # → build/libs/SFDrops-1.0.0.jar

cd SFSkilltree
./gradlew shadowJar   # → build/libs/SkillTreePlugin-1.0.0.jar
```

## Dependencias entre plugins

SFSkilltree depende de SFCore en tiempo de compilación:

```kotlin
// SFSkilltree/build.gradle.kts
compileOnly(fileTree("../SFCore/build/libs") { include("SFCore-*.jar") })
```

SFDrops depende de SFCore y SFSkilltree en compilación:

```kotlin
// SFDrops/build.gradle.kts
compileOnly(fileTree("../SFCore/build/libs") { include("SFCore-*.jar") })
compileOnly(fileTree("../SFSkilltree/build/libs") { include("*.jar") })
```