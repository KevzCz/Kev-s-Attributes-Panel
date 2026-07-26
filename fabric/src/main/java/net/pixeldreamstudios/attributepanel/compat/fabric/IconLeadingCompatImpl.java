package net.pixeldreamstudios.attributepanel.compat.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.pixeldreamstudios.attributepanel.compat.IconLeadingCompat;
import net.pixeldreamstudios.iconleadingtooltip.util.IconLeadingUtil;

public class IconLeadingCompatImpl {

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded("icon-leading-tooltip");
    }

    public static IconLeadingCompat.IconSplit extractIconImpl(String text) {
        try {
            String noCodes = IconLeadingUtil.stripSectionCodes(text);
            int[] span = IconLeadingUtil.firstIconSpan(noCodes);

            if (span[0] >= 0) {
                String icon = noCodes.substring(span[0], span[1]);
                String rest = noCodes.substring(0, span[0]) + noCodes.substring(span[1]);
                String clean = IconLeadingUtil.stripSectionCodes(rest).trim();
                return new IconLeadingCompat.IconSplit(clean, icon);
            }

            return new IconLeadingCompat.IconSplit(noCodes, null);
        } catch (Exception e) {
            return new IconLeadingCompat.IconSplit(text, null);
        }
    }

    public static boolean isIconGlyphImpl(int codepoint) {
        try {
            return IconLeadingUtil.isIconGlyph(codepoint);
        } catch (Exception e) {
            return false;
        }
    }

    public static String stripSectionCodesImpl(String text) {
        try {
            return IconLeadingUtil.stripSectionCodes(text);
        } catch (Exception e) {
            return text;
        }
    }
}