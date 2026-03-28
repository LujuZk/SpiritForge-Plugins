package dev.sfcharacter.api;

import dev.sfcharacter.managers.CharacterManager;
import dev.sfcharacter.models.CharacterData;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class SFCharacterAPI {

    private static SFCharacterAPI instance;
    private final CharacterManager manager;

    private SFCharacterAPI(CharacterManager manager) {
        this.manager = manager;
    }

    public static SFCharacterAPI get() {
        if (instance == null) throw new IllegalStateException("SFCharacter is not loaded");
        return instance;
    }

    public static void init(CharacterManager manager) {
        instance = new SFCharacterAPI(manager);
    }

    public static void shutdown() {
        instance = null;
    }

    public static boolean isAvailable() {
        return instance != null;
    }

    // ─── Queries ────────────────────────────────────────────────────────────

    public @Nullable CharacterData getActiveCharacter(UUID uuid) {
        return manager.getActiveCharacter(uuid);
    }

    public int getActiveSlot(UUID uuid) {
        return manager.hasActiveCharacter(uuid)
                ? manager.getActiveCharacter(uuid).slot()
                : -1;
    }

    public boolean isInCharacterSelection(UUID uuid) {
        return manager.isInCharacterSelection(uuid);
    }

    public List<CharacterData> getCharacters(UUID uuid) {
        return manager.getCharacters(uuid);
    }
}
