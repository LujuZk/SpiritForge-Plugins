package dev.sfcrafting;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class CharacterBridge {

    private final Plugin ownerPlugin;
    private final Plugin sfCharacterPlugin;
    private final ClassLoader sfCharacterLoader;
    private Method apiIsAvailableMethod;
    private Method apiGetMethod;
    private Method getActiveCharacterMethod;
    private Method characterClassMethod;
    private Method classDisplayNameMethod;
    private boolean initialized = false;

    public CharacterBridge(Plugin plugin) {
        this.ownerPlugin = plugin;
        this.sfCharacterPlugin = plugin.getServer().getPluginManager().getPlugin("SFCharacter");
        this.sfCharacterLoader = sfCharacterPlugin == null ? null : sfCharacterPlugin.getClass().getClassLoader();
    }

    public boolean isAvailable() {
        return sfCharacterPlugin != null;
    }

    private void ensureInitialized() {
        if (initialized || sfCharacterPlugin == null) {
            return;
        }
        try {
            Class<?> apiClass = Class.forName("dev.sfcharacter.api.SFCharacterAPI", true, sfCharacterLoader);
            apiIsAvailableMethod = apiClass.getMethod("isAvailable");
            apiGetMethod = apiClass.getMethod("get");
            getActiveCharacterMethod = apiClass.getMethod("getActiveCharacter", UUID.class);
            initialized = true;
            ownerPlugin.getLogger().info("SFCharacter bridge inicializado correctamente.");
        } catch (Exception e) {
            ownerPlugin.getLogger().warning("No se pudo inicializar SFCharacter bridge: " + e.getMessage());
        }
    }

    public String getActiveClassKey(Player player) {
        if (player == null || sfCharacterPlugin == null) {
            return "";
        }
        ensureInitialized();
        if (!initialized || apiGetMethod == null || getActiveCharacterMethod == null) {
            return "";
        }
        try {
            if (apiIsAvailableMethod != null) {
                boolean available = (boolean) apiIsAvailableMethod.invoke(null);
                if (!available) {
                    return "";
                }
            }
            Object api = apiGetMethod.invoke(null);
            if (api == null) {
                return "";
            }
            Object character = getActiveCharacterMethod.invoke(api, player.getUniqueId());
            if (character == null) {
                return "";
            }
            if (characterClassMethod == null) {
                characterClassMethod = character.getClass().getMethod("characterClass");
            }
            Object characterClass = characterClassMethod.invoke(character);
            if (characterClass == null) {
                return "";
            }
            return normalize(characterClass.toString());
        } catch (Exception e) {
            return "";
        }
    }

    public String getActiveClassDisplay(Player player) {
        if (player == null || sfCharacterPlugin == null) {
            return "Sin clase";
        }
        ensureInitialized();
        if (!initialized || apiGetMethod == null || getActiveCharacterMethod == null) {
            return "Sin clase";
        }
        try {
            if (apiIsAvailableMethod != null) {
                boolean available = (boolean) apiIsAvailableMethod.invoke(null);
                if (!available) {
                    return "Sin clase";
                }
            }
            Object api = apiGetMethod.invoke(null);
            if (api == null) {
                return "Sin clase";
            }
            Object character = getActiveCharacterMethod.invoke(api, player.getUniqueId());
            if (character == null) {
                return "Sin clase";
            }
            if (characterClassMethod == null) {
                characterClassMethod = character.getClass().getMethod("characterClass");
            }
            Object characterClass = characterClassMethod.invoke(character);
            if (characterClass == null) {
                return "Sin clase";
            }
            if (classDisplayNameMethod == null) {
                classDisplayNameMethod = characterClass.getClass().getMethod("getDisplayName");
            }
            Object displayName = classDisplayNameMethod.invoke(characterClass);
            if (displayName != null) {
                return displayName.toString();
            }
            return characterClass.toString();
        } catch (Exception e) {
            return "Sin clase";
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
