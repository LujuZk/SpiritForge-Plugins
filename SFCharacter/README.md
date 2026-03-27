# SFCharacter

Sistema de personajes para SpiritForge. Cada jugador puede crear hasta 5 personajes con datos completamente aislados (inventario, ubicacion, y en el futuro: stats, brujula, arbol de habilidades). Solo el ender chest es compartido entre personajes.

## Requisitos

- Paper 1.21.1+
- Java 21
- No requiere otros plugins de la suite (standalone)

## Flujo del jugador

1. Al unirse, el jugador es teleportado a un lobby y ve la GUI de seleccion de personajes
2. Durante la seleccion: movimiento, interaccion, items y comandos bloqueados
3. Slot vacio → elige clase (Mago, Guerrero, Picaro) → crea personaje → spawn del mundo
4. Slot ocupado → restaura inventario y ubicacion del personaje
5. `/character switch` → guarda estado actual → lobby → seleccion

## Comandos

| Comando | Descripcion |
|---------|-------------|
| `/character open` | Abre la GUI de seleccion (alias por defecto) |
| `/character switch` | Guarda el personaje actual y abre seleccion |
| `/character info` | Muestra info del personaje activo |

Aliases: `/personaje`, `/char`

## Configuracion

```yaml
database:
  file: characters.db    # Archivo SQLite

lobby:                    # Ubicacion del lobby de seleccion
  world: world
  x: 0.5
  y: 100.0
  z: 0.5
  yaw: 0.0
  pitch: 0.0
```

## API para otros plugins

### CharacterSelectEvent

Evento de Bukkit disparado cuando un jugador selecciona o crea un personaje:

```java
@EventHandler
public void onCharacterSelect(CharacterSelectEvent event) {
    Player player = event.getPlayer();
    CharacterData oldChar = event.getOldCharacter(); // null si es nuevo
    CharacterData newChar = event.getNewCharacter();
}
```

### SFCharacterAPI

Facade singleton para consultar datos de personajes:

```java
if (SFCharacterAPI.isAvailable()) {
    SFCharacterAPI api = SFCharacterAPI.get();
    CharacterData active = api.getActiveCharacter(playerUuid);
    List<CharacterData> chars = api.getCharacters(playerUuid);
    boolean inSelection = api.isInCharacterSelection(playerUuid);
}
```

## Schema de base de datos

- `characters` — datos basicos (uuid, slot, clase, nombre, fecha)
- `active_character` — slot activo por jugador
- `character_inventories` — inventario, armadura y offhand (BLOB)
- `character_locations` — mundo, x, y, z, yaw, pitch

## Build

```bash
./gradlew shadowJar
# Output: build/libs/SFCharacter-1.0.jar
```
