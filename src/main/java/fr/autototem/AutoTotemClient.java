package fr.autototem;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;

import java.util.List;

public final class AutoTotemClient implements ClientModInitializer {
    private static final String MOD_ID = "auto_totem";

    // The profiles are deliberately transparent test settings, not anti-cheat bypasses.
    private enum Profile {
        SAFE("Safe (2 ticks)", 2),
        FAST("Fast (1 tick)", 1),
        INSTANT("Instant (next client tick)", 0);

        final String label;
        final int delayTicks;

        Profile(String label, int delayTicks) {
            this.label = label;
            this.delayTicks = delayTicks;
        }

        Profile next() {
            Profile[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private KeyMapping toggleKey;
    private KeyMapping profileKey;
    private boolean enabled = false;
    private Profile profile = Profile.FAST;
    private int pendingTicks = -1;
    private long detectedAtNanos = 0L;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "controls")
        );

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.auto_totem.toggle",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_R,
                category
        ));

        profileKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.auto_totem.profile",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_G,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(Minecraft client) {
        while (toggleKey.consumeClick()) {
            enabled = !enabled;
            pendingTicks = -1;
            message(client, "Auto Totem " + (enabled ? "ON" : "OFF"));
        }

        while (profileKey.consumeClick()) {
            profile = profile.next();
            pendingTicks = -1;
            message(client, "Profil : " + profile.label);
        }

        if (!enabled || client.player == null || client.gameMode == null) {
            return;
        }

        // Wait for the server-confirmed offhand state; never fabricate inventory state.
        if (!client.player.getOffhandItem().isEmpty()) {
            pendingTicks = -1;
            return;
        }

        if (pendingTicks < 0) {
            pendingTicks = profile.delayTicks;
            detectedAtNanos = System.nanoTime();
        }

        if (pendingTicks > 0) {
            pendingTicks--;
            return;
        }

        int sourceMenuSlot = findTotemMenuSlot(client);
        if (sourceMenuSlot < 0) {
            pendingTicks = -1;
            return;
        }

        // Standard inventory SWAP click: button 40 is the offhand swap target.
        // The server still validates the normal inventory action.
        client.gameMode.handleInventoryMouseClick(
                client.player.inventoryMenu.containerId,
                sourceMenuSlot,
                40,
                ClickType.SWAP,
                client.player
        );

        long elapsedMicros = (System.nanoTime() - detectedAtNanos) / 1_000L;
        message(client, "Swap demandé — profil " + profile.name()
                + " — délai local observé " + elapsedMicros + " µs");
        pendingTicks = -1;
    }

    private int findTotemMenuSlot(Minecraft client) {
        List<Slot> slots = client.player.inventoryMenu.slots;

        // Player inventory menu: 9–35 main inventory, 36–44 hotbar.
        // Slots 0–8 are crafting/armor and are intentionally excluded.
        for (int menuSlot = 9; menuSlot <= 44 && menuSlot < slots.size(); menuSlot++) {
            if (slots.get(menuSlot).getItem().is(Items.TOTEM_OF_UNDYING)) {
                return menuSlot;
            }
        }
        return -1;
    }

    private void message(Minecraft client, String text) {
        if (client.player != null) {
            client.player.displayClientMessage(
                    Component.literal("[AutoTotem] " + text),
                    true
            );
        }
    }
}
