package net.pixeldreamstudios.attributepanel.compat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class IconLeadingCompat {
    private static final boolean ICON_LEADING_LOADED = FabricLoader.getInstance().isModLoaded("icon-leading-tooltip");


    public static boolean isLoaded() {
        return ICON_LEADING_LOADED;
    }

    public static class IconSplit {
        public final String cleanName;
        public final String leadingIcon;

        public IconSplit(String cleanName, String leadingIcon) {
            this.cleanName = cleanName;
            this.leadingIcon = leadingIcon;
        }

        public boolean hasIcon() {
            return leadingIcon != null && !leadingIcon.isEmpty();
        }
    }


    public static IconSplit extractIcon(String text) {
        if (!ICON_LEADING_LOADED || text == null || text.isEmpty()) {
            return new IconSplit(text, null);
        }

        try {

            String noCodes = net.pixeldreamstudios.iconleadingtooltip.util.IconLeadingUtil.stripSectionCodes(text);
            int[] span = net.pixeldreamstudios.iconleadingtooltip.util.IconLeadingUtil.firstIconSpan(noCodes);

            if (span[0] >= 0) {
                String icon = noCodes.substring(span[0], span[1]);
                String rest = noCodes.substring(0, span[0]) + noCodes.substring(span[1]);
                String clean = net.pixeldreamstudios.iconleadingtooltip.util.IconLeadingUtil.stripSectionCodes(rest).trim();
                return new IconSplit(clean, icon);
            }

            return new IconSplit(noCodes, null);
        } catch (Exception e) {
            return new IconSplit(text, null);
        }
    }

    public static IconSplit extractIcon(Text text) {
        if (text == null) {
            return new IconSplit("", null);
        }
        return extractIcon(text.getString());
    }

    public static IconSplit extractIconWithFallback(String displayName, String translationKey) {
        if (!ICON_LEADING_LOADED) {
            return new IconSplit(displayName, null);
        }

        IconSplit result = extractIcon(displayName);
        if (result.hasIcon()) {
            return result;
        }

        if (translationKey != null && !translationKey.isEmpty()) {
            result = extractIcon(translationKey);
            if (result.hasIcon()) {
                return result;
            }
        }

        return new IconSplit(displayName, null);
    }

    public static boolean isIconGlyph(int codepoint) {
        if (!ICON_LEADING_LOADED) {
            return false;
        }

        try {
            return net.pixeldreamstudios.iconleadingtooltip.util.IconLeadingUtil.isIconGlyph(codepoint);
        } catch (Exception e) {
            return false;
        }
    }
}