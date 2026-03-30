# SFCrafting

Sistema de crafteo y forja para SpiritForge. Implementa dos estaciones de trabajo (smelter y anvil) con rarezas, items calientes con temperatura, y efectos de aura cosmética.

## Requisitos

- Paper 1.21.1+
- Java 21
- Oraxen 1.21.0+

## Estaciones

### Smelter (Fundición)

Funde materias primas en lingotes calientes. Dos slots de input, un output con rareza basada en los ingredientes.

- Timer automático configurable por receta
- Output son "hot items" que se enfrían con el tiempo
- Dispara `SmeltCompleteEvent` al finalizar (escuchado por SFSkilltree para XP)

### Anvil (Forja)

Forja items usando moldes, lingotes calientes y materiales extra. El jugador golpea manualmente.

- Sistema de golpes con bonus por precisión
- Temperatura del lingote afecta el bonus por golpe
- Tiers: INCANDESCENTE > CALIENTE > TIBIO > FRIO

## Sistema de Rareza

5 niveles (0-4): Common, Uncommon, Rare, Epic, Legendary. Se determina por los ingredientes y se aplica al output.

## Hot Items

Los lingotes calientes tienen temperatura que decae con el tiempo:
- **En aire**: enfriamiento gradual (configurable por item)
- **En agua**: enfriamiento instantáneo
- Cada item define a qué se convierte al enfriarse (`cool-into`)

## Aura

Efecto cosmético de entidades `ItemDisplay` flotando sobre forjas activas.

## Comandos

| Comando | Descripción |
|---------|-------------|
| `/sfcrafting give <id> [cantidad] [rareza]` | Da un item con rareza opcional |
| `/sfcrafting reload` | Recarga configuración y auras |

Alias: `/cforge`

## Evento para otros plugins

```java
// SmeltCompleteEvent — disparado al completar una fundición
@EventHandler
public void onSmeltComplete(SmeltCompleteEvent event) {
    String recipeId = event.getRecipeId();    // "tin", "copper", "steel", etc.
    int rarity = event.getRarityLevel();       // 0-4
    Player player = event.getPlayer();
}
```

## Build

```bash
./gradlew shadowJar
# Output: build/libs/SFCrafting-1.0.0.jar
```
