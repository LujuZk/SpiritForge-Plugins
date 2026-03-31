package dev.sfcrafting;

import java.lang.reflect.Method;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class SkillBridge {

    private final Plugin skilltreePlugin;
    private final Plugin ownerPlugin;
    private final ClassLoader stpLoader;
    private Method getSkillManagerMethod;
    private Method getDataMethod;
    private Method fromKeyMethod;
    private Method getLevelMethod;
    private Class<?> skillTypeClass;
    private boolean initialized = false;

    public SkillBridge(Plugin plugin) {
        this.ownerPlugin = plugin;
        this.stpLoader = plugin.getServer().getPluginManager().getPlugin("SkillTreePlugin") != null
                ? plugin.getServer().getPluginManager().getPlugin("SkillTreePlugin").getClass().getClassLoader()
                : null;
        this.skilltreePlugin = plugin.getServer().getPluginManager().getPlugin("SkillTreePlugin");
    }

    public boolean isAvailable() {
        return skilltreePlugin != null;
    }

    private void ensureInitialized() {
        if (initialized || skilltreePlugin == null) return;
        try {
            getSkillManagerMethod = skilltreePlugin.getClass().getMethod("getSkillManager");
            skillTypeClass = Class.forName("dev.skilltree.models.SkillType", true, stpLoader);
            fromKeyMethod = skillTypeClass.getMethod("fromKey", String.class);
            getLevelMethod = Class.forName("dev.skilltree.models.PlayerSkillData", true, stpLoader)
                    .getMethod("getLevel", skillTypeClass);
            initialized = true;
            ownerPlugin.getLogger().info("SkillTreePlugin bridge inicializado correctamente.");
        } catch (Exception e) {
            ownerPlugin.getLogger().warning("No se pudo inicializar SkillTree bridge: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int getSkillLevel(Player player, String skillKey) {
        if (skilltreePlugin == null) return 0;
        
        ensureInitialized();
        if (!initialized || getSkillManagerMethod == null || getLevelMethod == null || fromKeyMethod == null) {
            return 0;
        }
        try {
            Object skillManager = getSkillManagerMethod.invoke(skilltreePlugin);
            if (skillManager == null) return 0;

            if (getDataMethod == null) {
                getDataMethod = skillManager.getClass().getMethod("getData", Player.class);
            }
            Object data = getDataMethod.invoke(skillManager, player);
            if (data == null) return 0;

            Object skillType = fromKeyMethod.invoke(null, skillKey);
            if (skillType == null) return 0;

            return (int) getLevelMethod.invoke(data, skillType);
        } catch (Exception e) {
            return 0;
        }
    }

    public String getSkillDisplayName(String skillKey) {
        if (skilltreePlugin == null) return skillKey;
        
        ensureInitialized();
        if (!initialized || fromKeyMethod == null) return skillKey;
        try {
            Object skillType = fromKeyMethod.invoke(null, skillKey);
            if (skillType == null) return skillKey;
            Method getDisplayName = skillTypeClass.getMethod("getDisplayName");
            return (String) getDisplayName.invoke(skillType);
        } catch (Exception e) {
            return skillKey;
        }
    }
}