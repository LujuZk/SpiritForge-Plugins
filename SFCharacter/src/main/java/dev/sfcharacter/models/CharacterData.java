package dev.sfcharacter.models;

import java.util.UUID;

public record CharacterData(
        UUID playerUuid,
        int slot,
        CharacterClass characterClass,
        String displayName,
        String createdAt
) {
}
