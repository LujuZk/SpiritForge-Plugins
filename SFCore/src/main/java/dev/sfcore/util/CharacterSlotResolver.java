package dev.sfcore.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

public final class CharacterSlotResolver {

    private static Boolean cached;
    private static Method methodGet;
    private static Method methodGetActiveSlot;

    private CharacterSlotResolver() {}

    public static int resolve(UUID uuid) {
        if (!isAvailable()) return 0;
        try {
            Object api = methodGet.invoke(null);
            int slot = (int) methodGetActiveSlot.invoke(api, uuid);
            return slot < 0 ? 0 : slot;
        } catch (Throwable t) {
            return 0;
        }
    }

    public static boolean isAvailable() {
        Boolean c = cached;
        if (c != null) return c;
        Plugin p = Bukkit.getPluginManager().getPlugin("SFCharacter");
        if (p == null || !p.isEnabled()) {
            cached = false;
            return false;
        }
        try {
            Class<?> apiClass = Class.forName("dev.sfcharacter.api.SFCharacterAPI");
            methodGet = apiClass.getMethod("get");
            methodGetActiveSlot = apiClass.getMethod("getActiveSlot", UUID.class);
            cached = true;
            return true;
        } catch (Throwable t) {
            cached = false;
            return false;
        }
    }

    public static void invalidate() {
        cached = null;
        methodGet = null;
        methodGetActiveSlot = null;
    }
}
