# SFCompass

Plugin de navegación por islas para SpiritForge. Los jugadores tienen un nivel de brújula que determina a qué islas pueden acceder. Las zonas restringidas muestran advertencias visuales (partículas, bossbar, oscuridad) y aplican daño progresivo.

## Características

- **Niveles de brújula** por jugador, persistidos en SQLite
- **Islas definidas en config.yml** con centro, radio, zona buffer y nivel requerido
- **Efectos visuales** al acercarse a zonas restringidas: pared de partículas, bossbar, oscuridad
- **Daño progresivo** al entrar en zonas sin nivel suficiente (escala con la profundidad)
- **Gracia tras respawn** para evitar daño inmediato al revivir

## Comandos

| Comando | Descripción | Permiso |
|---------|-------------|---------|
| `/compass give [jugador]` | Dar brújula al jugador | `sfcompass.admin` |
| `/compass setlevel <jugador> <nivel>` | Establecer nivel de brújula | `sfcompass.admin` |
| `/compass info [jugador]` | Ver nivel e islas accesibles | `sfcompass.use` |
| `/compass point <isla>` | Apuntar brújula a una isla | `sfcompass.use` |

Aliases: `/brujula`

## Configuración

Las islas se definen en `config.yml`:

```yaml
islands:
  nombre_isla:
    display-name: "Nombre visible"
    world: world
    center:
      x: 1000
      z: 1000
    radius: 100       # Radio del área de la isla
    buffer: 50        # Zona de advertencia antes del borde
    required-level: 2 # Nivel mínimo de brújula para entrar
```

Efectos de zona:

```yaml
zone-effects:
  damage-tick-interval: 10      # Ticks entre daño
  base-damage-percent: 0.05     # % de vida base por tick
  scale-factor: 5.0             # Escala de daño por profundidad
  blindness-duration: 60
  nausea-duration: 100

zone-warning:
  warning-radius: 30            # Distancia al borde para mostrar advertencias
  bossbar-enabled: true
  bossbar-color: RED
  particles-enabled: true
  particle-color: {r: 255, g: 80, b: 30}
  particle-size: 2.5
  particle-wall-spacing: 2.0
  darkness-enabled: true
  zone-particles-enabled: true
  zone-particle-count: 20
```

## Compilar

```bash
./gradlew shadowJar
# Output: build/libs/SFCompass-1.0.0.jar
```

## Requisitos

- Java 21
- Paper 1.21.1
