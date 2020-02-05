package codes.biscuit.skyblockaddons.renderer;

import codes.biscuit.skyblockaddons.SkyblockAddons;
import codes.biscuit.skyblockaddons.gui.LocationEditGui;
import codes.biscuit.skyblockaddons.gui.SkyblockAddonsGui;
import codes.biscuit.skyblockaddons.utils.EnumUtils;
import net.minecraft.client.Minecraft;

/**
 * This class renders the Skyblock Addons menus
 */
public class GuiRenderer extends Renderer{
    private EnumUtils.GUIType guiToOpen = null;
    private EnumUtils.GuiTab guiTabToOpen = EnumUtils.GuiTab.MAIN;
    private int guiPageToOpen = 1;
    private String textToOpen = null;

    /**
     * Creates a new instance of the SkyblockAddons renderer
     *
     * @param sbaIn the current SkyblockAddons instance
     */
    public GuiRenderer(SkyblockAddons sbaIn) {
        super(sbaIn);
    }

    /**
     * This renders the GUI.
     */
    public void renderGui() {
        if (guiToOpen == EnumUtils.GUIType.MAIN) {
            if (textToOpen == null) {
                Minecraft.getMinecraft().displayGuiScreen(new SkyblockAddonsGui(sba, guiPageToOpen, guiTabToOpen));
            } else {
                Minecraft.getMinecraft().displayGuiScreen(new SkyblockAddonsGui(sba, guiPageToOpen, guiTabToOpen, textToOpen));
                textToOpen = null;
            }
        } else if (guiToOpen == EnumUtils.GUIType.EDIT_LOCATIONS) {
            Minecraft.getMinecraft().displayGuiScreen(new LocationEditGui(sba, guiPageToOpen, guiTabToOpen, textToOpen));
        }
        guiToOpen = null;
    }

    public void setGuiToOpen(EnumUtils.GUIType guiToOpen, int page, EnumUtils.GuiTab tab) {
        this.guiToOpen = guiToOpen;
        guiPageToOpen = page;
        guiTabToOpen = tab;
    }

    public void setGuiToOpen(EnumUtils.GUIType guiToOpen, int page, EnumUtils.GuiTab tab, String text) {
        setGuiToOpen(guiToOpen,page,tab);
        textToOpen = text;
    }
}
