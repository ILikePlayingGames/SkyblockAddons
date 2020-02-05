package codes.biscuit.skyblockaddons.renderer;

import codes.biscuit.skyblockaddons.SkyblockAddons;
import codes.biscuit.skyblockaddons.gui.LocationEditGui;
import codes.biscuit.skyblockaddons.gui.buttons.ButtonLocation;
import codes.biscuit.skyblockaddons.utils.*;
import codes.biscuit.skyblockaddons.utils.nifty.ChatFormatting;
import codes.biscuit.skyblockaddons.utils.nifty.reflection.MinecraftReflection;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.GuiNotification;

import java.awt.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;

import static net.minecraft.client.gui.Gui.icons;

/**
 * This class renders the Skyblock Addons overlays.
 */
public class OverlayRenderer extends Renderer{
    /*
    Textures
     */
    private final static ResourceLocation BARS = new ResourceLocation("skyblockaddons", "bars.png");
    private final static ResourceLocation DEFENCE_VANILLA = new ResourceLocation("skyblockaddons", "defence.png");
    private final static ResourceLocation TEXT_ICONS = new ResourceLocation("skyblockaddons", "icons.png");
    private final static ResourceLocation IMPERIAL_BARS_FIX = new ResourceLocation("skyblockaddons", "imperialbarsfix.png");
    private final static ResourceLocation TICKER_SYMBOL = new ResourceLocation("skyblockaddons", "ticker.png");

    private static final SlayerArmorProgress[] DUMMY_PROGRESSES = new SlayerArmorProgress[]{new SlayerArmorProgress(new ItemStack(Items.diamond_boots)),
            new SlayerArmorProgress(new ItemStack(Items.chainmail_leggings)), new SlayerArmorProgress(new ItemStack(Items.diamond_chestplate)), new SlayerArmorProgress(new ItemStack(Items.leather_helmet))};
    private static java.util.List<ItemDiff> DUMMY_PICKUP_LOG = new ArrayList<>(Arrays.asList(new ItemDiff(ChatFormatting.DARK_PURPLE + "Forceful Ember Chestplate", 1),
            new ItemDiff("Boat", -1), new ItemDiff(ChatFormatting.BLUE + "Aspect of the End", 1)));

    @Setter
    private int arrowsLeft;
    @Setter private String cannotReachMobName = null;

    @Setter private EnumUtils.SkillType skill = null;
    @Setter private long skillFadeOutTime = -1;
    @Setter private String skillText = null;

    @Getter
    private DownloadInfo downloadInfo;

    private Feature subtitleFeature = null;
    @Getter @Setter private Feature titleFeature = null;

    public OverlayRenderer(SkyblockAddons sbaIn) {
        super(sbaIn);

        downloadInfo = new DownloadInfo(sba);
    }

    /**
     * I have an option so you can see the magma timer in other games so that's why.
     */
    public void renderTimersOnly() {
        if (!(mc.currentScreen instanceof LocationEditGui) && !(mc.currentScreen instanceof GuiNotification)) {
            GlStateManager.disableBlend();
            if (sba.getConfigValues().isEnabled(Feature.MAGMA_BOSS_TIMER) && sba.getConfigValues().isEnabled(Feature.SHOW_MAGMA_TIMER_IN_OTHER_GAMES) &&
                    sba.getPlayerListener().getMagmaAccuracy() != EnumUtils.MagmaTimerAccuracy.NO_DATA) {
                float scale = sba.getConfigValues().getGuiScale(Feature.MAGMA_BOSS_TIMER);
                GlStateManager.pushMatrix();
                GlStateManager.scale(scale, scale, 1);
                drawText(Feature.MAGMA_BOSS_TIMER, scale, mc, null);
                GlStateManager.popMatrix();
            }
            if (sba.getConfigValues().isEnabled(Feature.DARK_AUCTION_TIMER) && sba.getConfigValues().isEnabled(Feature.SHOW_DARK_AUCTION_TIMER_IN_OTHER_GAMES)) {
                float scale = sba.getConfigValues().getGuiScale(Feature.DARK_AUCTION_TIMER);
                GlStateManager.pushMatrix();
                GlStateManager.scale(scale, scale, 1);
                drawText(Feature.DARK_AUCTION_TIMER, scale, mc, null);
                GlStateManager.popMatrix();
            }
        }
    }

    /**
     * This renders all the different types gui text elements.
     */
    public void drawText(Feature feature, float scale, Minecraft mc, ButtonLocation buttonLocation) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        String text;
        int color = sba.getConfigValues().getColor(feature).getRGB();
        float textAlpha = 1;
        if (feature == Feature.MANA_TEXT) {
            text = getAttribute(Attribute.MANA) + "/" + getAttribute(Attribute.MAX_MANA);
        } else if (feature == Feature.HEALTH_TEXT) {
            text = getAttribute(Attribute.HEALTH) + "/" + getAttribute(Attribute.MAX_HEALTH);
        } else if (feature == Feature.DEFENCE_TEXT) {
            text = String.valueOf(getAttribute(Attribute.DEFENCE));
        } else if (feature == Feature.DEFENCE_PERCENTAGE) {
            double doubleDefence = getAttribute(Attribute.DEFENCE);
            double percentage = ((doubleDefence / 100) / ((doubleDefence / 100) + 1)) * 100; //Taken from https://hypixel.net/threads/how-armor-works-and-the-diminishing-return-of-higher-defence.2178928/
            BigDecimal bigDecimal = new BigDecimal(percentage).setScale(1, BigDecimal.ROUND_HALF_UP);
            text = bigDecimal.toString() + "%";
        } else if (feature == Feature.SPEED_PERCENTAGE) {
            String walkSpeed = String.valueOf(Minecraft.getMinecraft().thePlayer.capabilities.getWalkSpeed() * 1000);
            text = walkSpeed.substring(0, Math.min(walkSpeed.length(), 3));

            if (text.endsWith(".")) text = text.substring(0, text.indexOf('.')); //remove trailing periods

            text += "%";
        } else if (feature == Feature.HEALTH_UPDATES) {
            Integer healthUpdate = sba.getPlayerListener().getHealthUpdate();
            if (buttonLocation == null) {
                if (healthUpdate != null) {
                    color = healthUpdate > 0 ? ChatFormatting.GREEN.getRGB() : ChatFormatting.RED.getRGB();
                    text = (healthUpdate > 0 ? "+" : "-") + Math.abs(healthUpdate);
                } else {
                    return;
                }
            } else {
                text = "+123";
                color = ChatFormatting.GREEN.getRGB();
            }
        } else if (feature == Feature.DARK_AUCTION_TIMER) { // The timezone of the server, to avoid problems with like timezones that are 30 minutes ahead or whatnot.
            Calendar nextDarkAuction = Calendar.getInstance(TimeZone.getTimeZone("EST"));
            if (nextDarkAuction.get(Calendar.MINUTE) >= 55) {
                nextDarkAuction.add(Calendar.HOUR_OF_DAY, 1);
            }
            nextDarkAuction.set(Calendar.MINUTE, 55);
            nextDarkAuction.set(Calendar.SECOND, 0);
            int difference = (int) (nextDarkAuction.getTimeInMillis() - System.currentTimeMillis());
            int minutes = difference / 60000;
            int seconds = (int) Math.round((double) (difference % 60000) / 1000);
            StringBuilder timestamp = new StringBuilder();
            if (minutes < 10) {
                timestamp.append("0");
            }
            timestamp.append(minutes).append(":");
            if (seconds < 10) {
                timestamp.append("0");
            }
            timestamp.append(seconds);
            text = timestamp.toString();
        } else if (feature == Feature.MAGMA_BOSS_TIMER) {
            StringBuilder magmaBuilder = new StringBuilder();
            magmaBuilder.append(sba.getPlayerListener().getMagmaAccuracy().getSymbol());
            EnumUtils.MagmaTimerAccuracy ma = sba.getPlayerListener().getMagmaAccuracy();
            if (ma == EnumUtils.MagmaTimerAccuracy.ABOUT || ma == EnumUtils.MagmaTimerAccuracy.EXACTLY) {
                if (buttonLocation == null) {
                    int totalSeconds = sba.getPlayerListener().getMagmaTime();
                    if (totalSeconds < 0) totalSeconds = 0;
                    int hours = totalSeconds / 3600;
                    int minutes = totalSeconds / 60 % 60;
                    int seconds = totalSeconds % 60;
                    if (Math.abs(hours) >= 10) hours = 10;
                    magmaBuilder.append(hours).append(":");
                    if (minutes < 10) {
                        magmaBuilder.append("0");
                    }
                    magmaBuilder.append(minutes).append(":");
                    if (seconds < 10) {
                        magmaBuilder.append("0");
                    }
                    magmaBuilder.append(seconds);
                } else {
                    magmaBuilder.append("1:23:45");
                }
            }
            text = magmaBuilder.toString();
        } else if (feature == Feature.SKILL_DISPLAY) {
            if (buttonLocation == null) {
                text = skillText;
                if (text == null) return;
            } else {
                text = "+10 (20,000/50,000)";
            }
            if (buttonLocation == null) {
                int remainingTime = (int) (skillFadeOutTime - System.currentTimeMillis());
                if (remainingTime < 0) {
                    if (remainingTime < -2000) remainingTime = -2000;

                    textAlpha = (float) 1 - ((float) -remainingTime / 2000);
                    color = sba.getConfigValues().getColor(feature, Math.round(textAlpha * 255 >= 4 ? textAlpha * 255 : 4)).getRGB(); // so it fades out, 0.016 is the minimum alpha
                }
            }
        } else if(feature == Feature.ZEALOT_COUNTER) {
            if(sba.getUtils().getLocation() != Location.DRAGONS_NEST && buttonLocation == null) return;
            text = String.valueOf(sba.getPersistentValues().getKills());

            if (buttonLocation != null) text = "123";
        } else {
            return;
        }
        float x = sba.getConfigValues().getActualX(feature);
        float y = sba.getConfigValues().getActualY(feature);

        int height = 7;
        int width = MinecraftReflection.FontRenderer.getStringWidth(text);
        x -= Math.round(width * scale / 2);
        y -= Math.round(height * scale / 2);
        x /= scale;
        y /= scale;
        int intX = Math.round(x);
        int intY = Math.round(y);
        if (buttonLocation != null) {
            int boxXOne = intX - 4;
            int boxXTwo = intX + width + 4;
            int boxYOne = intY - 4;
            int boxYTwo = intY + height + 4;
            if (feature == Feature.MAGMA_BOSS_TIMER || feature == Feature.DARK_AUCTION_TIMER || feature == Feature.SKILL_DISPLAY) {
                boxXOne -= 18;
                boxYOne -= 2;
            } else if (feature == Feature.ZEALOT_COUNTER) {
                boxXOne -= 14;
                boxYOne -= 3;
                boxYTwo += 4;
            }
            buttonLocation.checkHoveredAndDrawBox(boxXOne, boxXTwo, boxYOne, boxYTwo, scale);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }

        GlStateManager.enableBlend();
        sba.getUtils().drawTextWithStyle(text, intX, intY, color, textAlpha);

        GlStateManager.color(1, 1, 1, 1);
        if (feature == Feature.DARK_AUCTION_TIMER) {
            mc.getTextureManager().bindTexture(TEXT_ICONS);
            Gui.drawModalRectWithCustomSizedTexture(intX - 18, intY - 5, 16, 0, 16, 16, 32, 32);
        } else if (feature == Feature.MAGMA_BOSS_TIMER) {
            mc.getTextureManager().bindTexture(TEXT_ICONS);
            Gui.drawModalRectWithCustomSizedTexture(intX - 18, intY - 5, 0, 0, 16, 16, 32, 32);
        } else if (feature == Feature.ZEALOT_COUNTER) {
            mc.getTextureManager().bindTexture(TEXT_ICONS);
            Gui.drawModalRectWithCustomSizedTexture(intX - 18, intY - 6, 0, 20, 20, 20, 40, 40);
        } else if (feature == Feature.SKILL_DISPLAY && ((skill != null && skill.getItem() != null) || buttonLocation != null)) {
            GlStateManager.enableRescaleNormal();
            RenderHelper.enableGUIStandardItemLighting();
            if (!(mc.currentScreen instanceof GuiChat)) {
                if (buttonLocation != null || textAlpha > 0.1) {
                    mc.getRenderItem().renderItemIntoGUI(buttonLocation == null ? skill.getItem() : EnumUtils.SkillType.FARMING.getItem(), intX - 18, intY - 5);
                }
            }
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
        }
    }

    public void drawRevenantIndicator(float scale, Minecraft mc, ButtonLocation buttonLocation) {
        float x = sba.getConfigValues().getActualX(Feature.SLAYER_INDICATOR);
        float y = sba.getConfigValues().getActualY(Feature.SLAYER_INDICATOR);

        int longest = -1;
        SlayerArmorProgress[] progresses = sba.getInventoryUtils().getSlayerArmorProgresses();
        if (buttonLocation != null) progresses = DUMMY_PROGRESSES;
        for (SlayerArmorProgress progress : progresses) {
            if (progress == null) continue;

            int textWidth = MinecraftReflection.FontRenderer.getStringWidth(progress.getProgressText());
            if (textWidth > longest) {
                longest = textWidth;
            }
        }
        if (longest == -1) return;

        int height = 15 * 4;
        int width = longest + 15;
        x -= Math.round(width * scale / 2);
        y -= Math.round(height * scale / 2);
        x /= scale;
        y /= scale;
        int intX = Math.round(x);
        int intY = Math.round(y);
        if (buttonLocation != null) {
            int boxXOne = intX - 4;
            int boxXTwo = intX + width + 4;
            int boxYOne = intY - 4;
            int boxYTwo = intY + height + 4;
            buttonLocation.checkHoveredAndDrawBox(boxXOne, boxXTwo, boxYOne, boxYTwo, scale);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }

        EnumUtils.AnchorPoint anchorPoint = sba.getConfigValues().getAnchorPoint(Feature.SLAYER_INDICATOR);
        boolean downwards = (anchorPoint == EnumUtils.AnchorPoint.TOP_LEFT || anchorPoint == EnumUtils.AnchorPoint.TOP_RIGHT);

        int drawnCount = 0;
        for (int armorPiece = 3; armorPiece >= 0; armorPiece--) {
            SlayerArmorProgress progress = progresses[downwards ? armorPiece : 3 - armorPiece];
            if (progress == null) continue;

            int fixedY;
            if (downwards) {
                fixedY = intY + drawnCount * 15;
            } else {
                fixedY = (intY + 45) - drawnCount * 15;
            }
            drawItemStack(mc, progress.getItemStack(), intX - 2, fixedY);
            sba.getUtils().drawTextWithStyle(progress.getProgressText(), intX + 17, fixedY + 5, 0xFFFFFFFF);
            drawnCount++;
        }
    }

    private void drawItemStack(Minecraft mc, ItemStack item, int x, int y) {
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemIntoGUI(item, x, y);
        RenderHelper.disableStandardItemLighting();
    }

    /**
     * This renders both the bars.
     */
    public void drawBar(Feature feature, float scale, Minecraft mc, ButtonLocation buttonLocation) {
        mc.getTextureManager().bindTexture(BARS);

        if (sba.getUtils().isUsingOldSkyBlockTexture()) {
            mc.getTextureManager().bindTexture(IMPERIAL_BARS_FIX);
        }

        // The height and width of this element (box not included)
        int barHeightExpansion = 2 * sba.getConfigValues().getSizes(feature).getY();
        int height = 3 + barHeightExpansion;

        int barWidthExpansion = 10 * sba.getConfigValues().getSizes(feature).getX();
        int width = 22 + barWidthExpansion;

        // The fill of the bar from 0 to 1
        float fill;
        if (feature == Feature.MANA_BAR) {
            fill = (float) getAttribute(Attribute.MANA) / getAttribute(Attribute.MAX_MANA);
        } else {
            fill = (float) getAttribute(Attribute.HEALTH) / getAttribute(Attribute.MAX_HEALTH);
        }
        if (fill > 1) fill = 1;
        int filled = Math.round(fill * width);

        float x = sba.getConfigValues().getActualX(feature);
        float y = sba.getConfigValues().getActualY(feature);
        Color color = sba.getConfigValues().getColor(feature);

        if (feature == Feature.HEALTH_BAR && sba.getConfigValues().isEnabled(Feature.CHANGE_BAR_COLOR_FOR_POTIONS)) {
            if (mc.thePlayer.isPotionActive(19/* Poison */)) {
                color = ChatFormatting.DARK_GREEN.getColor();
            } else if (mc.thePlayer.isPotionActive(20/* Wither */)) {
                color = ChatFormatting.DARK_GRAY.getColor();
            }
        }

        // Put the x & y to scale, remove half the width and height to center this element.
        x /= scale;
        y /= scale;
        x -= (float) width / 2;
        y -= (float) height / 2;
        int intX = Math.round(x);
        int intY = Math.round(y);
        if (buttonLocation == null) {
            drawModularBar(mc, color, false, intX, intY + barHeightExpansion / 2, null, feature, filled, width);
            if (filled > 0) {
                drawModularBar(mc, color, true, intX, intY + barHeightExpansion / 2, null, feature, filled, width);
            }
        } else {
            int boxXOne = intX - 4;
            int boxXTwo = intX + width + 5;
            int boxYOne = intY - 3;
            int boxYTwo = intY + height + 4;
            buttonLocation.checkHoveredAndDrawBox(boxXOne, boxXTwo, boxYOne, boxYTwo, scale);
            drawModularBar(mc, color, false, intX, intY + barHeightExpansion / 2, buttonLocation, feature, filled, width);
            if (filled > 0) {
                drawModularBar(mc, color, true, intX, intY + barHeightExpansion / 2, buttonLocation, feature, filled, width);
            }
        }
    }

    private void drawModularBar(Minecraft mc, Color color, boolean filled, int x, int y, ButtonLocation buttonLocation, Feature feature, int fillWidth, int maxWidth) {
        Gui gui = mc.ingameGUI;
        if (buttonLocation != null) {
            gui = buttonLocation;
        }
        if (color.getRGB() == ChatFormatting.BLACK.getRGB()) {
            GlStateManager.color(0.25F, 0.25F, 0.25F); // too dark normally
        } else { // a little darker for contrast
            GlStateManager.color(((float) color.getRed() / 255) * 0.9F, ((float) color.getGreen() / 255) * 0.9F, ((float) color.getBlue() / 255) * 0.9F);
        }
        CoordsPair sizes = sba.getConfigValues().getSizes(feature);
        if (!filled) fillWidth = maxWidth;
        drawBarStart(gui, x, y, filled, sizes.getX(), sizes.getY(), fillWidth, color, maxWidth);
    }

    private void drawBarStart(Gui gui, int x, int y, boolean filled, int barWidth, int barHeight, int fillWidth, Color color, int maxWidth) {
        int baseTextureY = filled ? 0 : 6;

//        drawMiddleThreeRows(gui,x+10,y,barHeight,22,baseTextureY,2, fillWidth, 2); // these two lines just fill some gaps in the bar
//        drawMiddleThreeRows(gui,x+11+(barWidth*9),y,barHeight,22,baseTextureY,2, fillWidth, 2);

        drawAllFiveRows(gui, x, y, barHeight, 0, baseTextureY, 11, fillWidth);

        drawBarSeparators(gui, x + 11, y, baseTextureY, barWidth, barHeight, fillWidth);

        if (fillWidth < maxWidth-1 && fillWidth > 0 && // This just draws a dark line to easily distinguish where the bar's progress is.
                sba.getUtils().isUsingDefaultBarTextures()) { // It doesn't always work out nicely when using like custom textures though.
            GlStateManager.color(((float) color.getRed() / 255) * 0.8F, ((float) color.getGreen() / 255) * 0.8F, ((float) color.getBlue() / 255) * 0.8F);
            drawMiddleThreeRows(gui, x + fillWidth, y, barHeight, 11, 6, 2, fillWidth, 2);
        }
    }

    private void drawMiddleBarParts(Gui gui, int x, int y, int baseTextureY, int barWidth, int barHeight, int fillWidth) {
        int endBarX = 0;
        for (int i = 0; i < barWidth; i++) {
            endBarX = x + (i * 10);
            drawAllFiveRows(gui, endBarX, y, barHeight, 12, baseTextureY, 9, fillWidth - 11 - (i * 10));
        }
        drawBarEnd(gui, endBarX + 10, y, baseTextureY, barWidth, barHeight, fillWidth);
    }

    private void drawBarSeparators(Gui gui, int x, int y, int baseTextureY, int barWidth, int barHeight, int fillWidth) {
        for (int i = 0; i <= barWidth; i++) {
            drawMiddleThreeRows(gui, x + (i * 10), y, barHeight, 11, baseTextureY, 1, fillWidth - 11 - (i * 10), 2);
        }
        drawMiddleBarParts(gui, x + 1, y, baseTextureY, barWidth, barHeight, fillWidth);
    }

    private void drawBarEnd(Gui gui, int x, int y, int baseTextureY, int barWidth, int barHeight, int fillWidth) {
        drawAllFiveRows(gui, x, y, barHeight, 22, baseTextureY, 11, fillWidth - 11 - (barWidth * 10));
    }

    private void drawAllFiveRows(Gui gui, int x, int y, int barHeight, int textureX, int baseTextureY, int width, int fillWidth) {
        if (fillWidth > width || baseTextureY >= 8) fillWidth = width;
        gui.drawTexturedModalRect(x, y + 1 - barHeight, textureX, baseTextureY, fillWidth, 1);

        drawMiddleThreeRows(gui, x, y, barHeight, textureX, baseTextureY, width, fillWidth, 1);

        gui.drawTexturedModalRect(x, y + 3 + barHeight, textureX, baseTextureY + 4, fillWidth, 1);
    }

    private void drawMiddleThreeRows(Gui gui, int x, int y, int barHeight, int textureX, int baseTextureY, int width, int fillWidth, int rowHeight) {
        if (fillWidth > width || baseTextureY >= 8) fillWidth = width;
        for (int i = 0; i < barHeight; i++) {
            if (rowHeight == 2) { //this means its drawing bar separators, and its a little different
                gui.drawTexturedModalRect(x, y - i, textureX, baseTextureY, fillWidth, rowHeight);
            } else {
                gui.drawTexturedModalRect(x, y + 1 - i, textureX, baseTextureY + 1, fillWidth, rowHeight);
            }
        }

        gui.drawTexturedModalRect(x, y + 2, textureX, baseTextureY + 2, fillWidth, 1);

        for (int i = 0; i < barHeight; i++) {
            gui.drawTexturedModalRect(x, y + 3 + i, textureX, baseTextureY + 3, fillWidth, rowHeight);
        }
    }

    public void drawUpdateMessage() {
        EnumUtils.UpdateMessageType messageType = downloadInfo.getMessageType();
        if (messageType != null) {
            Minecraft mc = Minecraft.getMinecraft();
            String[] textList;
            if (messageType == EnumUtils.UpdateMessageType.PATCH_AVAILABLE || messageType == EnumUtils.UpdateMessageType.MAJOR_AVAILABLE) {
                textList = downloadInfo.getMessageType().getMessages(downloadInfo.getNewestVersion());
            } else if (messageType == EnumUtils.UpdateMessageType.DOWNLOADING) {
                textList = downloadInfo.getMessageType().getMessages(String.valueOf(downloadInfo.getDownloadedBytes()), String.valueOf(downloadInfo.getTotalBytes()));
            } else if (messageType == EnumUtils.UpdateMessageType.DOWNLOAD_FINISHED) {
                textList = downloadInfo.getMessageType().getMessages(downloadInfo.getOutputFileName());
            } else {
                textList = downloadInfo.getMessageType().getMessages();
            }
            int halfWidth = new ScaledResolution(mc).getScaledWidth() / 2;
            Gui.drawRect(halfWidth - 110, 20, halfWidth + 110, 53 + textList.length * 10, sba.getUtils().getDefaultBlue(140));
            String text = "SkyblockAddons";
            GlStateManager.pushMatrix();
            float scale = 1.5F;
            GlStateManager.scale(scale, scale, 1);
            MinecraftReflection.FontRenderer.drawString(text, (int) (halfWidth / scale) - MinecraftReflection.FontRenderer.getStringWidth(text) / 2, (int) (30 / scale), ChatFormatting.WHITE);
            GlStateManager.popMatrix();
            int y = 45;
            for (String line : textList) {
                MinecraftReflection.FontRenderer.drawString(line, halfWidth - MinecraftReflection.FontRenderer.getStringWidth(line) / 2, y, ChatFormatting.WHITE);
                y += 10;
            }
        }
    }

    /**
     * This renders a bar for the skeleton hat bones bar.
     */
    public void drawSkeletonBar(Minecraft mc, float scale, ButtonLocation buttonLocation) {
        float x = sba.getConfigValues().getActualX(Feature.SKELETON_BAR);
        float y = sba.getConfigValues().getActualY(Feature.SKELETON_BAR);
        int bones = 0;

        if (!(mc.currentScreen instanceof LocationEditGui)) {
            for (Entity listEntity : mc.theWorld.loadedEntityList) {
                if (listEntity instanceof EntityItem &&
                        listEntity.ridingEntity instanceof EntityArmorStand && listEntity.ridingEntity.isInvisible() && listEntity.getDistanceToEntity(mc.thePlayer) <= 8) {
                    bones++;
                }
            }
        } else {
            bones = 3;
        }
        if (bones > 3) bones = 3;

        float height = 16;
        float width = 3 * 15;
        x -= Math.round(width * scale / 2);
        y -= Math.round(height * scale / 2);
        x /= scale;
        y /= scale;
        if (buttonLocation != null) {
            int boxXOne = Math.round(x - 4);
            int boxXTwo = Math.round(x + width + 4);
            int boxYOne = Math.round(y - 4);
            int boxYTwo = Math.round(y + height + 4);
            buttonLocation.checkHoveredAndDrawBox(boxXOne, boxXTwo, boxYOne, boxYTwo, scale);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }

        for (int boneCounter = 0; boneCounter < bones; boneCounter++) {
            mc.getRenderItem().renderItemIntoGUI(new ItemStack(Items.bone), Math.round((x + boneCounter * 15)), Math.round(y));
        }
    }

    /**
     * This renders the skeleton bar.
     */
    public void drawScorpionFoilTicker(Minecraft mc, float scale, ButtonLocation buttonLocation) {
        if (buttonLocation != null || sba.getPlayerListener().getTickers() != -1) {
            float x = sba.getConfigValues().getActualX(Feature.TICKER_CHARGES_DISPLAY);
            float y = sba.getConfigValues().getActualY(Feature.TICKER_CHARGES_DISPLAY);

            float height = 9;
            float width = 4 * 11;
            x -= Math.round(width * scale / 2);
            y -= Math.round(height * scale / 2);
            x /= scale;
            y /= scale;
            if (buttonLocation != null) {
                int boxXOne = Math.round(x - 4);
                int boxXTwo = Math.round(x + width + 2);
                int boxYOne = Math.round(y - 4);
                int boxYTwo = Math.round(y + height + 4);
                buttonLocation.checkHoveredAndDrawBox(boxXOne, boxXTwo, boxYOne, boxYTwo, scale);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            }

            int maxTickers = (buttonLocation == null) ? sba.getPlayerListener().getMaxTickers() : 4;
            for (int tickers = 0; tickers < maxTickers; tickers++) { //main.getPlayerListener().getTickers()
                mc.getTextureManager().bindTexture(TICKER_SYMBOL);
                GlStateManager.enableAlpha();
                if (tickers < (buttonLocation == null ? sba.getPlayerListener().getTickers() : 3)) {
                    Gui.drawModalRectWithCustomSizedTexture(Math.round(x + tickers * 11), Math.round(y), 0, 0, 9, 9, 18, 9);
                } else {
                    Gui.drawModalRectWithCustomSizedTexture(Math.round(x + tickers * 11), Math.round(y), 9, 0, 9, 9, 18, 9);
                }
            }
        }
    }

    /**
     * This renders the defence icon.
     */
    public void drawIcon(float scale, Minecraft mc, ButtonLocation buttonLocation) {
        if (sba.getConfigValues().isDisabled(Feature.USE_VANILLA_TEXTURE_DEFENCE)) {
            mc.getTextureManager().bindTexture(icons);
        } else {
            mc.getTextureManager().bindTexture(DEFENCE_VANILLA);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        // The height and width of this element (box not included)
        int height = 9;
        int width = 9;
        float x = sba.getConfigValues().getActualX(Feature.DEFENCE_ICON);
        float y = sba.getConfigValues().getActualY(Feature.DEFENCE_ICON);
        if (buttonLocation == null) {
            float newScale = scale * 1.5F;
            GlStateManager.pushMatrix();
            GlStateManager.scale(newScale, newScale, 1);
            newScale *= scale;
            x -= Math.round((float) width * newScale / 2);
            y -= Math.round((float) height * newScale / 2);
            mc.ingameGUI.drawTexturedModalRect(x / newScale, y / newScale, 34, 9, width, height);
            GlStateManager.popMatrix();
        } else {
            scale *= (scale / 1.5);
            x -= Math.round((float) width * scale / 2);
            y -= Math.round((float) height * scale / 2);
            x /= scale;
            y /= scale;
            int intX = Math.round(x);
            int intY = Math.round(y);
            int boxXOne = intX - 2;
            int boxXTwo = intX + width + 2;
            int boxYOne = intY - 2;
            int boxYTwo = intY + height + 2;
            buttonLocation.checkHoveredAndDrawBox(boxXOne, boxXTwo, boxYOne, boxYTwo, scale);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            buttonLocation.drawTexturedModalRect(intX, intY, 34, 9, width, height);
        }
    }


    public void drawPotionEffectTimers(float scale, ButtonLocation buttonLocation){
        float x = sba.getConfigValues().getActualX(Feature.TAB_EFFECT_TIMERS);
        float y = sba.getConfigValues().getActualY(Feature.TAB_EFFECT_TIMERS);

        TabEffectManager tabEffect = TabEffectManager.getInstance();

        java.util.List<String> potionTimers = tabEffect.getPotionTimers();
        java.util.List<String> powerupTimers = tabEffect.getPowerupTimers();

        if(potionTimers.isEmpty() && powerupTimers.isEmpty()) {
            if (buttonLocation == null) {
                return;
            } else { // When editing GUI draw dummy timers.
                potionTimers = TabEffectManager.getDummyPotionTimers();
                powerupTimers = TabEffectManager.getDummyPowerupTimers();
            }
        }

        EnumUtils.AnchorPoint anchorPoint = sba.getConfigValues().getAnchorPoint(Feature.TAB_EFFECT_TIMERS);
        boolean topDown = (anchorPoint == EnumUtils.AnchorPoint.TOP_LEFT || anchorPoint == EnumUtils.AnchorPoint.TOP_RIGHT);

        int totalEffects = potionTimers.size() + powerupTimers.size();
        int spacer = (!potionTimers.isEmpty() && !powerupTimers.isEmpty()) ? 3 : 0;

        int height = (totalEffects * 9) + spacer; //9 px per effect + 3px spacer between Potions and Powerups if both exist
        int width = 156; //String width of "Enchanting XP Boost III 1:23:45"
        x-=Math.round(width*scale/2);
        y-=Math.round(height*scale/2)-(totalEffects * 4);
        x/=scale;
        y/=scale;
        int intX = Math.round(x);
        int intY = Math.round(y);
        if (buttonLocation != null) {
            int boxXOne = intX-4;
            int boxXTwo = intX+width+4;
            int boxYOffset = topDown ? 0 : (1-totalEffects) * 9;
            int boxYOne = intY-4 + boxYOffset;
            int boxYTwo = intY+4+height - 3 + boxYOffset;
            buttonLocation.checkHoveredAndDrawBox(boxXOne, boxXTwo, boxYOne, boxYTwo, scale);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }

        int drawnCount = 0;
        for(String potion : potionTimers){
            int fixedY = intY + (topDown ? 0 : spacer) + (drawnCount * 9);
            sba.getUtils().drawTextWithStyle(potion, intX, fixedY, ChatFormatting.WHITE);
            drawnCount += topDown ? 1 : -1;
        }
        for(String powerUp : powerupTimers){
            int fixedY = intY + (topDown ? spacer : 0) + (drawnCount * 9);
            sba.getUtils().drawTextWithStyle(powerUp, intX, fixedY, ChatFormatting.WHITE);
            drawnCount += topDown ? 1 : -1;
        }
    }

    public void drawItemPickupLog(float scale, ButtonLocation buttonLocation) {
        float x = sba.getConfigValues().getActualX(Feature.ITEM_PICKUP_LOG);
        float y = sba.getConfigValues().getActualY(Feature.ITEM_PICKUP_LOG);

        EnumUtils.AnchorPoint anchorPoint = sba.getConfigValues().getAnchorPoint(Feature.ITEM_PICKUP_LOG);
        boolean downwards = anchorPoint == EnumUtils.AnchorPoint.TOP_RIGHT || anchorPoint == EnumUtils.AnchorPoint.TOP_LEFT;

        int height = 8 * 3;
        int width = MinecraftReflection.FontRenderer.getStringWidth("+ 1x Forceful Ember Chestplate");
        x -= Math.round(width * scale / 2);
        y -= Math.round(height * scale / 2);
        x /= scale;
        y /= scale;
        int intX = Math.round(x);
        int intY = Math.round(y);
        if (buttonLocation != null) {
            int boxXOne = intX - 4;
            int boxXTwo = intX + width + 4;
            int boxYOne = intY - 4;
            int boxYTwo = intY + height + 4;
            buttonLocation.checkHoveredAndDrawBox(boxXOne, boxXTwo, boxYOne, boxYTwo, scale);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
        int i = 0;
        Collection<ItemDiff> log = sba.getInventoryUtils().getItemPickupLog();
        if (buttonLocation != null) {
            log = DUMMY_PICKUP_LOG;
        }
        for (ItemDiff itemDiff : log) {
            String text = String.format("%s %sx §r%s", itemDiff.getAmount() > 0 ? "§a+" : "§c-",
                    Math.abs(itemDiff.getAmount()), itemDiff.getDisplayName());
            int stringY = intY + (i * MinecraftReflection.FontRenderer.getFontHeight());
            if (!downwards) {
                stringY = intY - (i * MinecraftReflection.FontRenderer.getFontHeight());
                stringY += 18;
            }

            GlStateManager.enableBlend();
            sba.getUtils().drawTextWithStyle(text, intX, stringY, ChatFormatting.WHITE);
            i++;
        }
    }

    public void drawPowerOrbStatus(Minecraft mc, float scale, ButtonLocation buttonLocation) {
        PowerOrbManager.Entry activePowerOrb = PowerOrbManager.getInstance().get();
        if (buttonLocation != null) {
            activePowerOrb = PowerOrbManager.DUMMY_ENTRY;
        }
        if (activePowerOrb != null) {
            PowerOrb powerOrb = activePowerOrb.getPowerOrb();
            int seconds = activePowerOrb.getSeconds();

            EnumUtils.PowerOrbDisplayStyle displayStyle = sba.getConfigValues().getPowerOrbDisplayStyle();
            if (displayStyle == EnumUtils.PowerOrbDisplayStyle.DETAILED) {
                drawDetailedPowerOrbStatus(mc, scale, buttonLocation, powerOrb, seconds);
            } else {
                drawCompactPowerOrbStatus(mc, scale, buttonLocation, powerOrb, seconds);
            }
        }
    }

    /**
     * Displays the power orb display in a compact way with only the amount of seconds to the right of the icon.
     *
     *  --
     * |  | XXs
     *  --
     */
    private void drawCompactPowerOrbStatus(Minecraft mc, float scale, ButtonLocation buttonLocation, PowerOrb powerOrb, int seconds) {
        float x = sba.getConfigValues().getActualX(Feature.POWER_ORB_STATUS_DISPLAY);
        float y = sba.getConfigValues().getActualY(Feature.POWER_ORB_STATUS_DISPLAY);

        String secondsString = String.format("§e%ss", seconds);
        int spacing = 1;
        int iconSize = MinecraftReflection.FontRenderer.getFontHeight() * 3; // 3 because it looked the best
        int width = iconSize + spacing + MinecraftReflection.FontRenderer.getStringWidth(secondsString);
        // iconSize also acts as height
        x -= Math.round(width * scale / 2);
        y -= Math.round(iconSize * scale / 2);
        x /= scale;
        y /= scale;
        int intX = Math.round(x);
        int intY = Math.round(y);

        if (buttonLocation != null) {
            int boxXOne = intX - 4;
            int boxXTwo = intX + width + 4;
            int boxYOne = intY - 4;
            int boxYTwo = intY + iconSize + 4;
            buttonLocation.checkHoveredAndDrawBox(boxXOne, boxXTwo, boxYOne, boxYTwo, scale);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }

        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        mc.getTextureManager().bindTexture(powerOrb.resourceLocation);
        GlStateManager.color(1, 1, 1, 1F);
        Gui.drawModalRectWithCustomSizedTexture(intX, intY, 0, 0, iconSize, iconSize, iconSize, iconSize);
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();

        sba.getUtils().drawTextWithStyle(secondsString, intX + iconSize, intY + (iconSize / 2) - (MinecraftReflection.FontRenderer.getFontHeight() / 2), ChatFormatting.WHITE.getColor(255).getRGB());
    }

    /**
     * Displays the power orb with detailed stats about the boost you're receiving.
     *
     *  --  +X ❤/s
     * |  | +X ✎/s
     *  --  +X ❁
     *  XXs
     */
    private void drawDetailedPowerOrbStatus(Minecraft mc, float scale, ButtonLocation buttonLocation, PowerOrb powerOrb, int seconds) {
        float x = sba.getConfigValues().getActualX(Feature.POWER_ORB_STATUS_DISPLAY);
        float y = sba.getConfigValues().getActualY(Feature.POWER_ORB_STATUS_DISPLAY);

        String secondsString = String.format("§e%ss", seconds);
        int spacing = 1;
        int iconSize = MinecraftReflection.FontRenderer.getFontHeight() * 3; // 3 because it looked the best
        int iconAndSecondsHeight = iconSize + MinecraftReflection.FontRenderer.getFontHeight();

        int maxHealth = sba.getUtils().getAttributes().get(Attribute.MAX_HEALTH).getValue();
        double healthRegen = maxHealth * powerOrb.healthRegen;
        double healIncrease = powerOrb.healIncrease * 100;

        List<String> display = new LinkedList<>();
        display.add(String.format("§c+%s ❤/s", Utils.niceDouble(healthRegen, 2)));
        if(powerOrb.manaRegen > 0) {
            int maxMana = sba.getUtils().getAttributes().get(Attribute.MAX_MANA).getValue();
            double manaRegen = maxMana / 50F;
            manaRegen = manaRegen + manaRegen * powerOrb.manaRegen;
            display.add(String.format("§b+%s ✎/s", Utils.niceDouble(manaRegen, 2)));
        }
        if (powerOrb.strength > 0) {
            display.add(String.format("§4+%d ❁", powerOrb.strength));
        }
        if (healIncrease > 0) {
            display.add(String.format("§2+%s%% Healing", Utils.niceDouble(healIncrease, 2)));
        }

        Optional<String> longestLine = display.stream().max(Comparator.comparingInt(String::length));

        int effectsHeight = (MinecraftReflection.FontRenderer.getFontHeight() + spacing) * display.size();
        int width = iconSize + longestLine.map(MinecraftReflection.FontRenderer::getStringWidth)
                .orElseGet(() -> MinecraftReflection.FontRenderer.getStringWidth(display.get(0)));
        int height = Math.max(effectsHeight, iconAndSecondsHeight);
        x -= Math.round(width * scale / 2);
        y -= Math.round(24 * scale / 2);
        x /= scale;
        y /= scale;
        int intX = Math.round(x);
        int intY = Math.round(y);

        if (buttonLocation != null) {
            int boxXOne = intX - 4;
            int boxXTwo = intX + width + 4;
            int boxYOne = intY - 4;
            int boxYTwo = intY + height + 4;
            buttonLocation.checkHoveredAndDrawBox(boxXOne, boxXTwo, boxYOne, boxYTwo, scale);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }

        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        mc.getTextureManager().bindTexture(powerOrb.resourceLocation);
        GlStateManager.color(1, 1, 1, 1F);
        Gui.drawModalRectWithCustomSizedTexture(intX, intY, 0, 0, iconSize, iconSize, iconSize, iconSize);
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();

        sba.getUtils().drawTextWithStyle(secondsString, intX + (iconSize / 2) - (MinecraftReflection.FontRenderer.getStringWidth(secondsString) / 2), intY + iconSize, ChatFormatting.WHITE.getColor(255).getRGB());

        int startY = Math.round(intY + (iconAndSecondsHeight / 2f) - (effectsHeight / 2f));
        for (int i = 0; i < display.size(); i++) {
            sba.getUtils().drawTextWithStyle(display.get(i), intX + iconSize + 3, startY + (i * (MinecraftReflection.FontRenderer.getFontHeight() + spacing)), ChatFormatting.WHITE.getColor(255).getRGB());
        }
    }

    /**
     * This renders all the gui elements (bars, icons, texts, skeleton bar, etc.).
     */
    public void renderOverlays() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof LocationEditGui) && !(mc.currentScreen instanceof GuiNotification)) {
            GlStateManager.disableBlend();

            for (Feature feature : Feature.getGuiFeatures()) {
                if (sba.getConfigValues().isEnabled(feature)) {
                    if (feature == Feature.SKELETON_BAR && !sba.getInventoryUtils().isWearingSkeletonHelmet())
                        continue;
                    if (feature == Feature.HEALTH_UPDATES && sba.getPlayerListener().getHealthUpdate() == null)
                        continue;

                    float scale = sba.getConfigValues().getGuiScale(feature);
                    GlStateManager.pushMatrix();
                    GlStateManager.scale(scale, scale, 1);
                    feature.draw(scale, mc, null);
                    GlStateManager.popMatrix();
                }
            }
        }
    }

    /**
     * This renders all the title/subtitle warnings from features.
     */
    public void renderWarnings(ScaledResolution scaledResolution) {
        int i = scaledResolution.getScaledWidth();
        if (titleFeature != null) {
            int j = scaledResolution.getScaledHeight();
            GlStateManager.pushMatrix();
            GlStateManager.translate((float) (i / 2), (float) (j / 2), 0.0F);
//            GlStateManager.enableBlend();
//            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.pushMatrix();
            GlStateManager.scale(4.0F, 4.0F, 4.0F);
            Message message = null;
            switch (titleFeature) {
                case MAGMA_WARNING:
                    message = Message.MESSAGE_MAGMA_BOSS_WARNING;
                    break;
                case FULL_INVENTORY_WARNING:
                    message = Message.MESSAGE_FULL_INVENTORY;
                    break;
                case SUMMONING_EYE_ALERT:
                    message = Message.MESSAGE_SUMMONING_EYE_FOUND;
                    break;
                case SPECIAL_ZEALOT_ALERT:
                    message = Message.MESSAGE_SPECIAL_ZEALOT_FOUND;
                    break;
            }
            if (message != null) {
                String text = message.getMessage();
                MinecraftReflection.FontRenderer.drawString(text, (float) (-MinecraftReflection.FontRenderer.getStringWidth(text) / 2), -20.0F,
                        sba.getConfigValues().getColor(titleFeature).getRGB(), true);
            }
            GlStateManager.popMatrix();
            GlStateManager.popMatrix();
        }
        if (subtitleFeature != null) {
            int j = scaledResolution.getScaledHeight();
            GlStateManager.pushMatrix();
            GlStateManager.translate((float) (i / 2), (float) (j / 2), 0.0F);
            GlStateManager.pushMatrix();
            GlStateManager.scale(2.0F, 2.0F, 2.0F);
            Message message = null;
            switch (subtitleFeature) {
                case MINION_STOP_WARNING:
                    message = Message.MESSAGE_MINION_CANNOT_REACH;
                    break;
                case MINION_FULL_WARNING:
                    message = Message.MESSAGE_MINION_IS_FULL;
                    break;
                case NO_ARROWS_LEFT_ALERT:
                    message = Message.MESSAGE_NO_ARROWS_LEFT;
                    break;
            }
            if (message != null) {
                String text;
                if (message == Message.MESSAGE_MINION_CANNOT_REACH) {
                    text = message.getMessage(cannotReachMobName);
                } else if (subtitleFeature == Feature.NO_ARROWS_LEFT_ALERT && arrowsLeft != -1) {
                    text = Message.MESSAGE_ONLY_FEW_ARROWS_LEFT.getMessage(Integer.toString(arrowsLeft));
                } else {
                    text = message.getMessage();
                }
                MinecraftReflection.FontRenderer.drawString(text, (float) (-MinecraftReflection.FontRenderer.getStringWidth(text) / 2), -23.0F,
                        sba.getConfigValues().getColor(subtitleFeature).getRGB(), true);
            }
            GlStateManager.popMatrix();
            GlStateManager.popMatrix();
        }
    }

    public void setSubtitleFeature(Feature subtitleFeature) {
        this.subtitleFeature = subtitleFeature;
        if (subtitleFeature == null) {
            this.arrowsLeft = -1;
        }
    }

    /*
     * Easily grab an attribute from utils.
     */
    private int getAttribute(Attribute attribute) {
        return sba.getUtils().getAttributes().get(attribute).getValue();
    }
}
