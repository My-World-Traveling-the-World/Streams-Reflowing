package dev.streamsreflowing.fabric.mixin;

import dev.streamsreflowing.compat.streamscontrol.client.StreamsUnifiedConfigScreen;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$GameTab")
public abstract class StreamsCreateWorldScreenMixin extends GridLayoutTab {
    protected StreamsCreateWorldScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void plaincraft$addStreamsConfigButton(CallbackInfo ci) {
        Button configButton = Button.builder(
                        Component.translatable("plaincraft.streams.config_button"),
                        button -> {
                            Minecraft minecraft = Minecraft.getInstance();
                            if (minecraft != null && minecraft.screen instanceof CreateWorldScreen screen) {
                                minecraft.setScreen(new StreamsUnifiedConfigScreen((Screen) screen));
                            }
                        })
                .width(210)
                .build();
        int roadWeaverRow = SharedConstants.getCurrentVersion().isStable() ? 4 : 5;
        this.layout.addChild(configButton, roadWeaverRow + 1, 0,
                this.layout.newCellSettings().alignHorizontallyCenter());
    }
}
