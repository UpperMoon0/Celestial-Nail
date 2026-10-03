package com.nstut.celestialnail.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Mod("celestial_nail_menu_probe")
public final class MenuProbe {
    private int stage, frames;
    public MenuProbe() {
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::render);
    }
    private void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (mc.getOverlay() != null) return;
        if (stage == 0 && mc.screen instanceof TitleScreen) {
            mc.setScreen(new OptionsScreen(mc.screen, mc.options));
            stage = 1;
        } else if (stage == 1) {
            for (var child : mc.screen.children()) {
                if (child instanceof Button button && button.getMessage().getString().contains("Video Settings")) {
                    button.onPress();
                    stage = 2;
                    return;
                }
            }
            throw new IllegalStateException("Video Settings button missing");
        }
    }
    private void render(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END && stage == 3 && ++frames >= 40) {
            try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
                image.writeToFile(Path.of("menu-evidence/extras-page.png"));
                stage = 4;
            } catch (Exception error) { throw new IllegalStateException(error); }
        }
        if (event.phase != TickEvent.Phase.END || stage != 2 || ++frames < 20) return;
        var mc = Minecraft.getInstance();
        try {
            Class<?> menuType = mc.screen.getClass();
            while (menuType != null && !menuType.getName().endsWith("SodiumOptionsGUI")
                    && !menuType.getName().endsWith("EmbeddiumVideoOptionsScreen")) menuType = menuType.getSuperclass();
            if (menuType == null)
                throw new IllegalStateException("Wrong video screen: " + mc.screen.getClass());
            var field = menuType.getDeclaredField("pages");
            field.setAccessible(true);
            var names = new ArrayList<String>();
            Object extra = null;
            for (var page : (List<?>)field.get(mc.screen)) {
                var name = ((Component)page.getClass().getMethod("getName").invoke(page)).getString();
                names.add(name);
                if (page.getClass().getName().startsWith("toni.sodiumextras.")) extra = page;
            }
            if (extra == null) throw new IllegalStateException("Extras page missing: " + names);
            var out = Path.of("menu-evidence");
            Files.createDirectories(out);
            try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                image.writeToFile(out.resolve("embeddium-menu.png"));
            }
            if (menuType.getName().endsWith("EmbeddiumVideoOptionsScreen")) {
                var selected = menuType.getDeclaredField("tabFrameSelectedTab");
                selected.setAccessible(true);
                @SuppressWarnings("unchecked")
                var tab = (java.util.concurrent.atomic.AtomicReference<Component>)selected.get(null);
                tab.set((Component)extra.getClass().getMethod("getName").invoke(extra));
                mc.setScreen(mc.screen);
            } else {
                menuType.getMethod("setPage", Class.forName("me.jellysquid.mods.sodium.client.gui.options.OptionPage")).invoke(mc.screen, extra);
            }
            Files.writeString(out.resolve("result.txt"), "PASS: Video Settings opens " + mc.screen.getClass().getName() + "\nPages: " + names);
            stage = 3;
        } catch (Exception error) { throw new IllegalStateException("Development menu verification failed", error); }
    }
}
