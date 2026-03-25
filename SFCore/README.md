# SFCore

Plugin central de stats para SpiritForge. Provee una API para que otros plugins registren y consulten bonuses de estadísticas de jugadores, con persistencia SQLite y eventos Bukkit.

## Características

- **26 stats** organizados en 3 modos de aplicación
- **API pública** para agregar/quitar/consultar bonuses desde otros plugins
- **Persistencia** automática en SQLite (`stats.db`)
- **StatChangeEvent** para que otros plugins reaccionen a cambios de stats
- **Comandos de test** para verificar stats en tiempo real sin reiniciar el server

## Stats disponibles

| Modo | Stats |
|------|-------|
| Atributo (multiplicador) | `DAMAGE_BONUS`, `MOVEMENT_SPEED` |
| Atributo (plano) | `DAMAGE_REDUCTION`, `MAX_HEALTH` |
| Listener custom | `LIFESTEAL`, `MINING_SPEED`, `BLEED`, `STUN`, `ARMOR_PIERCE`, `EXECUTE`, `REGEN_ON_HIT`, `KILL_STREAK_DAMAGE`, `COUNTERATTACK`, `FRENZY`, `BERSERKER_DAMAGE`, `AREA_DAMAGE`, `OPENER_DAMAGE`, `GLOBAL_MULTIPLIER`, `PATH_AMPLIFIER`, `VEIN_MINER`, `DOUBLE_DROP`, `FORTUNE_BONUS`, `AUTO_PICKUP`, `EXPLOSION`, `ORE_DETECTION`, `STR`, `VIT`, `INT`, `AGI` |

## API para otros plugins

```gradle
// build.gradle.kts
compileOnly(files("../../Server/plugins/SFCore-1.0.0.jar"))
```

```yaml
# plugin.yml
depend: [SFCore]
```

```java
SFCoreAPI api = SFCoreAPI.get();

// Agregar un bonus
api.addBonus(player, "myplugin:skill:node_x", StatType.DAMAGE_BONUS, 0.15);

// Agregar múltiples (más eficiente — un solo reapply)
api.addBonuses(player, List.of(
    new StatBonus("myplugin:buff:str", StatType.STR, 5.0),
    new StatBonus("myplugin:buff:agi", StatType.AGI, 3.0)
));

// Consultar total
double dmg = api.getTotal(player, StatType.DAMAGE_BONUS);

// Limpiar todos los bonuses de un plugin
api.clearSource(player, "myplugin:");
```

El source sigue el formato `"pluginname:category:id"`.

## Comandos

| Comando | Descripción | Permiso |
|---------|-------------|---------|
| `/sfcore stats [player]` | Ver todos los stats del jugador | `sfcore.admin` |
| `/sfcore test <stat\|all> on` | Activar monitor de stat en tiempo real | `sfcore.admin` |
| `/sfcore test <stat\|all> off` | Desactivar monitor y limpiar bonus de test | `sfcore.admin` |
| `/sfcore test <stat> add <valor>` | Agregar bonus de test temporal | `sfcore.admin` |

Aliases de stat: `damage`/`dmg`, `armor`, `health`/`hp`, `speed`, `mining`, `ls`

## Compilar

```bash
./gradlew shadowJar
# Output: build/libs/SFCore-1.0.0.jar
```

## Requisitos

- Java 21
- Paper 1.21.1
