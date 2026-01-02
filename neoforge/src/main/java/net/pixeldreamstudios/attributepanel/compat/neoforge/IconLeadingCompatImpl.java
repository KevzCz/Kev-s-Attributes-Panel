package net.pixeldreamstudios.attributepanel.compat.neoforge;

import net.neoforged.fml.ModList;
import net.pixeldreamstudios.attributepanel.compat.IconLeadingCompat;

public class IconLeadingCompatImpl {

    public static boolean isLoaded() {
        return ModList.get().isLoaded("icon_leading_tooltip");
    }

    public static IconLeadingCompat.IconSplit extractIconImpl(String text) {
        try {
            String noCodes = net.pixeldreamstudios.iconleadingtooltip.util.IconLeadingUtil.stripSectionCodes(text);
            int[] span = net.pixeldreamstudios.iconleadingtooltip.util.IconLeadingUtil.firstIconSpan(noCodes);

            if (span[0] >= 0) {
                String icon = noCodes.substring(span[0], span[1]);
                String rest = noCodes.substring(0, span[0]) + noCodes.substring(span[1]);
                String clean = net.pixeldreamstudios.iconleadingtooltip.util.IconLeadingUtil.stripSectionCodes(rest).trim();
                return new IconLeadingCompat.IconSplit(clean, icon);
            }

            return new IconLeadingCompat.IconSplit(noCodes, null);
        } catch (Exception e) {
            return new IconLeadingCompat.IconSplit(text, null);
        }
    }

    public static boolean isIconGlyphImpl(int codepoint) {
        try {
            return net.pixeldreamstudios.iconleadingtooltip.util.IconLeadingUtil.isIconGlyph(codepoint);
        } catch (Exception e) {
            return false;
        }
    }

    public static String stripSectionCodesImpl(String text) {
        try {
            return net.pixeldreamstudios.iconleadingtooltip.util.IconLeadingUtil.stripSectionCodes(text);
        } catch (Exception e) {
            return text;
        }
    }
}