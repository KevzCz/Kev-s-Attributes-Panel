package net.pixeldreamstudios.attributepanel.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.network.chat.Component;

public class IconLeadingCompat {

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

    @ExpectPlatform
    public static boolean isLoaded() {
        throw new AssertionError();
    }

    public static IconSplit extractIcon(String text) {
        if (!isLoaded() || text == null || text.isEmpty()) {
            return new IconSplit(text, null);
        }

        return extractIconImpl(text);
    }


    @ExpectPlatform
    public static IconSplit extractIconImpl(String text) {
        throw new AssertionError();
    }

    public static IconSplit extractIcon(Component text) {
        if (text == null) {
            return new IconSplit("", null);
        }
        return extractIcon(text.getString());
    }


    public static IconSplit extractIconWithFallback(String displayName, String translationKey) {
        if (!isLoaded()) {
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
        if (!isLoaded()) {
            return false;
        }

        return isIconGlyphImpl(codepoint);
    }


    @ExpectPlatform
    protected static boolean isIconGlyphImpl(int codepoint) {
        throw new AssertionError();
    }


    public static String stripSectionCodes(String text) {
        if (!isLoaded() || text == null) {
            return text;
        }

        return stripSectionCodesImpl(text);
    }

    @ExpectPlatform
    protected static String stripSectionCodesImpl(String text) {
        throw new AssertionError();
    }
}