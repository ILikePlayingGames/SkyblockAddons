package codes.biscuit.skyblockaddons.renderer;

import codes.biscuit.skyblockaddons.SkyblockAddons;
import net.minecraft.client.Minecraft;

/**
 * This class contains common code used in {@link GuiRenderer} and {@link OverlayRenderer}
 */
public class Renderer {
    Minecraft mc;
    SkyblockAddons sba;

    public Renderer(SkyblockAddons sbaIn) {
        this.mc = Minecraft.getMinecraft();
        this.sba = sbaIn;
    }
}
