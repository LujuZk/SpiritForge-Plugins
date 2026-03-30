# SFDrops

Plugin de drops de ores/logs con rareza gaussiana para SpiritForge. La rareza se calcula en base a stats del jugador, nivel de skill y poder del pico.

## Requisitos

- Paper 1.21.1+
- Java 21
- Oraxen 1.21.0+ (requerido)
- SFCore (opcional — aporta STR y MINING_SPEED)
- SkillTreePlugin (opcional — aporta nivel de skill)

## Sistema de Rareza

5 niveles: Common, Uncommon, Rare, Epic, Legendary.

La probabilidad de cada rareza se calcula con una curva gaussiana basada en un score compuesto:

| Factor | Peso | Max |
|--------|------|-----|
| STR (SFCore) | 30% | 100 |
| MINING_SPEED (SFCore) | 30% | 200 |
| Class bonus (SFSkilltree) | 20% | 100 |
| Pickaxe power | 20% | 10 |

Si SFCore o SFSkilltree no están presentes, esos factores defaultean a 0.

## Configuración

```yaml
gaussian:
  width: 0.8
  curveExponent: 1.5

weights:
  str: 0.30
  mining: 0.30
  class: 0.20
  pickaxe: 0.20

oreDrops:
  iron_ore: RAW_IRON
  copper_ore: raw_copper_ore
  tin_ore: raw_tin_ore
```

## Comandos

| Comando | Descripción |
|---------|-------------|
| `/sfdrops reload` | Recarga configuración |
| `/sfdrops debug` | Toggle debug mode por jugador |

## Build

```bash
./gradlew shadowJar
# Output: build/libs/SFDrops-1.0.0.jar
```
