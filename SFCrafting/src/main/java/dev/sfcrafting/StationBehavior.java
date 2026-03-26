package dev.sfcrafting;

import org.bukkit.entity.Player;

public interface StationBehavior {
    ForgeState.StationType type();
    void handleButtonClick(ForgeManager manager, ForgeState state, Player player);
    void updateButton(ForgeManager manager, ForgeState state);
    boolean isInputSlot(int slot);
    boolean isOutputSlot(int slot);
    boolean isButtonSlot(int slot);
    int outputSlot();
}

