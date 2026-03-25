# SFSkilltree

Plugin RPG de árbol de habilidades para SpiritForge. Los jugadores ganan XP en 7 tipos de skill, suben de nivel, y gastan puntos en nodos de un árbol visual con rutas, requisitos previos y elecciones exclusivas.

## Características

- **7 tipos de skill:** Minería, Agricultura, Pesca, Espada, Hacha, Arco, Tridente
- **Árbol de nodos visual:** Branching con rutas, prerequisitos y exclusividades
- **XP configurable** por actividad/combate; nivelación con fórmula configurable
- **GUI de doble cofre** con texturas custom via Oraxen
- **Editor web** drag-and-drop para diseñar árboles (React + Vite)
- **Config-driven:** Árboles definidos en YAML, sin recompilar para cambiar nodos
- **Integración con SFCore:** Los nodos aplican bonuses de stats via `SFCoreAPI`

## Requisitos

- Java 21
- Paper 1.21.1
- Oraxen 1.21.0+ (items visuales y glifos GUI)
- SFCore (API de stats)

## Estructura de archivos de config

```
plugins/SFSkilltree/
├── skilltree.db           # Datos de jugadores (SQLite, auto-generado)
├── config.yml             # XP, niveles, nombres de skills, mensajes
├── icons.yml              # Mapeo nombre lógico → Oraxen item ID (st_*)
└── skills/
    └── <nombre>/
        └── tree.yml       # Definición del árbol (nodos, grid, edges)
```

## Formato de tree.yml

```yaml
id: sword
display-name: "⚔ Espada"
skill-type: SWORD

nodes:
  nodo_id:
    name: "Nombre del nodo"
    cost: 1
    icon: sword_basic        # clave en icons.yml
    requires: [otro_id]      # prerequisitos
    exclusive-with: [alt_id] # bloquea otras rutas
    effect-type: damage_bonus
    effect-value: 0.1

grid:
  - page: 0
    cells:
      - {col: 0, row: 2, type: node, node-id: nodo_id}
      - {col: 1, row: 2, type: connector, connector-id: connector_h}

edges:
  - {from: nodo1, to: nodo2}
```

## Comandos

| Comando | Descripción | Permiso |
|---------|-------------|---------|
| `/skills` | Abrir menú de habilidades | `skilltree.use` |
| `/skillsadmin reset <jugador>` | Resetear progreso del jugador | `skilltree.admin` |
| `/skillsadmin give <jugador> <skill> <cantidad>` | Otorgar puntos de skill | `skilltree.admin` |
| `/skillsadmin info <jugador>` | Ver stats del jugador | `skilltree.admin` |

Aliases: `/skill`, `/habilidades`

## Integración con Oraxen

Los items visuales de la GUI (nodos, conectores, backgrounds) son items de Oraxen con prefijo `st_`:

- Texturas: `Server/plugins/Oraxen/pack/textures/skilltree/`
- Modelos: `Server/plugins/Oraxen/pack/models/skilltree/`
- Items: `Server/plugins/Oraxen/items/skilltree.yml`
- Glifos: `Server/plugins/Oraxen/glyphs/skilltree.yml`

El mapeo de nombre lógico a `st_*` se define en `icons.yml`. No se necesita un resource pack separado.

## Editor web

```bash
cd editor
npm install
npm run dev   # Dev server en http://localhost:5173/SFSkilltree/
```

El editor exporta YAML compatible con el formato `tree.yml` que el plugin lee directamente.

## Compilar

```bash
./gradlew shadowJar
# Output: build/libs/SkillTreePlugin-1.0.0.jar
```
