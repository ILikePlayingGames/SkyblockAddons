package codes.biscuit.skyblockaddons.listeners;

import codes.biscuit.skyblockaddons.SkyblockAddons;
import codes.biscuit.skyblockaddons.renderer.GuiRenderer;
import codes.biscuit.skyblockaddons.renderer.OverlayRenderer;
import codes.biscuit.skyblockaddons.utils.Feature;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class RenderListener {

    private SkyblockAddons main;

    @Getter @Setter private boolean predictHealth = false;
    @Getter @Setter private boolean predictMana = false;

    private GuiRenderer guiRenderer;
    private OverlayRenderer overlayRenderer;

    /**
     * Creates a new instance of {@code RenderListener}
     *
     * @param main the current SkyblockAddons instance
     */
    public RenderListener(SkyblockAddons main) {
        this.main = main;
        guiRenderer = main.getGuiRenderer();
        overlayRenderer = main.getOverlayRenderer();
    }

    /**
     * Render overlays and warnings for clients without labymod.
     */
    @SubscribeEvent()
    public void onRenderRegular(RenderGameOverlayEvent.Post e) {
        if ((!main.isUsingLabymod() || Minecraft.getMinecraft().ingameGUI instanceof GuiIngameForge)) {
            if (e.type == RenderGameOverlayEvent.ElementType.EXPERIENCE || e.type == RenderGameOverlayEvent.ElementType.JUMPBAR) {
                if (main.getUtils().isOnSkyblock()) {
                    overlayRenderer.renderOverlays();
                    overlayRenderer.renderWarnings(e.resolution);
                } else {
                    overlayRenderer.renderTimersOnly();
                }
                overlayRenderer.drawUpdateMessage();
            }
        }
    }

    /**
     * Render overlays and warnings for clients with labymod.
     * Labymod creates its own ingame gui and replaces the forge one, and changes the events that are called.
     * This is why the above method can't work for both.
     */
    @SubscribeEvent()
    public void onRenderLabyMod(RenderGameOverlayEvent e) {
        if (e.type == null && main.isUsingLabymod()) {
            if (main.getUtils().isOnSkyblock()) {
                overlayRenderer.renderOverlays();
                overlayRenderer.renderWarnings(e.resolution);
            } else {
                overlayRenderer.renderTimersOnly();
            }
            overlayRenderer.drawUpdateMessage();
        }
    }

    @SubscribeEvent()
    public void onRenderLiving(RenderLivingEvent.Specials.Pre<? extends EntityLivingBase> e) {
        Entity entity = e.entity;
        if (main.getConfigValues().isEnabled(Feature.MINION_DISABLE_LOCATION_WARNING) && entity.hasCustomName()) {
            if (entity.getCustomNameTag().startsWith("§cThis location isn\'t perfect! :(")) {
                e.setCanceled(true);
            }
            if (entity.getCustomNameTag().startsWith("§c/!\\")) {
                for (Entity listEntity : Minecraft.getMinecraft().theWorld.loadedEntityList) {
                    if (listEntity.hasCustomName() && listEntity.getCustomNameTag().startsWith("§cThis location isn\'t perfect! :(") &&
                            listEntity.posX == entity.posX && listEntity.posZ == entity.posZ &&
                            listEntity.posY + 0.375 == entity.posY) {
                        e.setCanceled(true);
                        break;
                    }
                }
            }
        }
    }

    @SubscribeEvent()
    public void onRenderRemoveBars(RenderGameOverlayEvent.Pre e) {
        if (e.type == RenderGameOverlayEvent.ElementType.ALL) {
            if (main.getUtils().isOnSkyblock()) {
                if (main.getConfigValues().isEnabled(Feature.HIDE_FOOD_ARMOR_BAR)) {
                    GuiIngameForge.renderFood = false;
                    GuiIngameForge.renderArmor = false;
                }
                if (main.getConfigValues().isEnabled(Feature.HIDE_HEALTH_BAR)) {
                    GuiIngameForge.renderHealth = false;
                }
                if (main.getConfigValues().isEnabled(Feature.HIDE_PET_HEALTH_BAR)) {
                    GuiIngameForge.renderHealthMount = false;
                }
            } else {
                if (main.getConfigValues().isEnabled(Feature.HIDE_HEALTH_BAR)) {
                    GuiIngameForge.renderHealth = true;
                }
                if (main.getConfigValues().isEnabled(Feature.HIDE_FOOD_ARMOR_BAR)) {
                    GuiIngameForge.renderArmor = true;
                }
            }
        }
    }

    @SubscribeEvent()
    public void onRender(TickEvent.RenderTickEvent e) {
        guiRenderer.renderGui();
    }
}
