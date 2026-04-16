package dev.sfcrafting;

import java.lang.reflect.Method;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class SFCoreBridge {

    private final Plugin owner;
    private final Plugin sfCorePlugin;
    private final ClassLoader sfCoreLoader;
    private boolean initialized = false;
    private Method apiIsAvailableMethod;
    private Method apiGetMethod;
    private Method getManaMethod;
    private Method getMaxManaMethod;
    private Method spendManaMethod;

    public SFCoreBridge(Plugin owner) {
        this.owner = owner;
        this.sfCorePlugin = owner.getServer().getPluginManager().getPlugin("SFCore");
        this.sfCoreLoader = sfCorePlugin == null ? null : sfCorePlugin.getClass().getClassLoader();
    }

    public boolean isAvailable() {
        return sfCorePlugin != null;
    }

    private void initIfNeeded() {
        if (initialized || sfCorePlugin == null) {
            return;
        }
        try {
            Class<?> apiClass = Class.forName("dev.sfcore.api.SFCoreAPI", true, sfCoreLoader);
            apiIsAvailableMethod = apiClass.getMethod("isAvailable");
            apiGetMethod = apiClass.getMethod("get");
            initialized = true;
            owner.getLogger().info("SFCore bridge inicializado correctamente.");
        } catch (Exception e) {
            owner.getLogger().warning("No se pudo inicializar SFCore bridge: " + e.getMessage());
        }
    }

    private Object getApi() {
        initIfNeeded();
        if (!initialized || apiGetMethod == null) {
            return null;
        }
        try {
            if (apiIsAvailableMethod != null) {
                boolean available = (boolean) apiIsAvailableMethod.invoke(null);
                if (!available) {
                    return null;
                }
            }
            return apiGetMethod.invoke(null);
        } catch (Exception e) {
            return null;
        }
    }

    public double getMana(Player player) {
        Object api = getApi();
        if (api == null) {
            return 0.0D;
        }
        try {
            if (getManaMethod == null) {
                getManaMethod = api.getClass().getMethod("getMana", Player.class);
            }
            Object value = getManaMethod.invoke(api, player);
            return value instanceof Number n ? n.doubleValue() : 0.0D;
        } catch (Exception e) {
            return 0.0D;
        }
    }

    public double getMaxMana(Player player) {
        Object api = getApi();
        if (api == null) {
            return 0.0D;
        }
        try {
            if (getMaxManaMethod == null) {
                getMaxManaMethod = api.getClass().getMethod("getMaxMana", Player.class);
            }
            Object value = getMaxManaMethod.invoke(api, player);
            return value instanceof Number n ? n.doubleValue() : 0.0D;
        } catch (Exception e) {
            return 0.0D;
        }
    }

    public boolean spendMana(Player player, double amount) {
        Object api = getApi();
        if (api == null) {
            return true;
        }
        try {
            if (spendManaMethod == null) {
                spendManaMethod = api.getClass().getMethod("spendMana", Player.class, double.class);
            }
            Object result = spendManaMethod.invoke(api, player, amount);
            return result instanceof Boolean b && b;
        } catch (Exception e) {
            return false;
        }
    }
}
