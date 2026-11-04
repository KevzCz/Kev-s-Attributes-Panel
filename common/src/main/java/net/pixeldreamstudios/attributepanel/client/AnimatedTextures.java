package net.pixeldreamstudios.attributepanel.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.gui.GuiMetadataSection;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.pixeldreamstudios.attributepanel.KevsAttributesPanel;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public final class AnimatedTextures {
    private static final long MILLIS_PER_TICK = 1000L / SharedConstants.TICKS_PER_SECOND;
    private static final String OWN_PREFIX = "animated_textures/";
    private static final String PATH_SEPARATOR = "/";
    private static final int CHANNEL_BITS = 8;
    private static final int CHANNEL_MASK = 255;
    private static final int PIXEL_BITS = 32;
    private static final Map<ResourceLocation, Optional<Animation>> CACHE = new HashMap<>();
    private static final Map<ResourceLocation, Optional<GuiSpriteScaling>> SCALING = new HashMap<>();
    private static boolean listenerRegistered = false;
    private static long frameTick;

    private AnimatedTextures() {
    }

    public static void startFrame() {
        frameTick = Util.getMillis() / MILLIS_PER_TICK;
    }

    public static boolean exists(ResourceLocation texture) {
        return scalingFor(texture).isPresent();
    }

    public static void blitScaled(GuiGraphics ctx, ResourceLocation texture, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        GuiSpriteScaling scaling = scalingFor(texture).orElse(null);
        if (scaling instanceof GuiSpriteScaling.Tile tile) {
            tile(ctx, texture, x, y, width, height, 0, 0, tile.width(), tile.height(), tile.width(), tile.height());
        } else if (scaling instanceof GuiSpriteScaling.NineSlice slice) {
            nineSlice(ctx, texture, x, y, width, height, slice);
        } else {
            blit(ctx, texture, x, y, width, height);
        }
    }

    private static void nineSlice(GuiGraphics ctx, ResourceLocation texture, int x, int y, int width, int height, GuiSpriteScaling.NineSlice slice) {
        GuiSpriteScaling.NineSlice.Border border = slice.border();
        int texWidth = slice.width();
        int texHeight = slice.height();
        int left = Math.min(border.left(), width / 2);
        int right = Math.min(border.right(), width / 2);
        int top = Math.min(border.top(), height / 2);
        int bottom = Math.min(border.bottom(), height / 2);
        int innerWidth = width - left - right;
        int innerHeight = height - top - bottom;
        int texInnerWidth = texWidth - border.left() - border.right();
        int texInnerHeight = texHeight - border.top() - border.bottom();
        int rightU = texWidth - right;
        int bottomV = texHeight - bottom;
        int rightX = x + width - right;
        int bottomY = y + height - bottom;
        int innerX = x + left;
        int innerY = y + top;

        region(ctx, texture, x, y, left, top, 0, 0, texWidth, texHeight);
        region(ctx, texture, rightX, y, right, top, rightU, 0, texWidth, texHeight);
        region(ctx, texture, x, bottomY, left, bottom, 0, bottomV, texWidth, texHeight);
        region(ctx, texture, rightX, bottomY, right, bottom, rightU, bottomV, texWidth, texHeight);
        tile(ctx, texture, innerX, y, innerWidth, top, border.left(), 0, texInnerWidth, top, texWidth, texHeight);
        tile(ctx, texture, innerX, bottomY, innerWidth, bottom, border.left(), bottomV, texInnerWidth, bottom, texWidth, texHeight);
        tile(ctx, texture, x, innerY, left, innerHeight, 0, border.top(), left, texInnerHeight, texWidth, texHeight);
        tile(ctx, texture, rightX, innerY, right, innerHeight, rightU, border.top(), right, texInnerHeight, texWidth, texHeight);
        tile(ctx, texture, innerX, innerY, innerWidth, innerHeight, border.left(), border.top(), texInnerWidth, texInnerHeight, texWidth, texHeight);
    }

    private static void tile(GuiGraphics ctx, ResourceLocation texture, int x, int y, int width, int height, int u, int v, int regionWidth, int regionHeight, int texWidth, int texHeight) {
        if (width <= 0 || height <= 0 || regionWidth <= 0 || regionHeight <= 0) {
            return;
        }
        for (int row = 0; row < height; row += regionHeight) {
            int rowHeight = Math.min(regionHeight, height - row);
            for (int column = 0; column < width; column += regionWidth) {
                region(ctx, texture, x + column, y + row, Math.min(regionWidth, width - column), rowHeight, u, v, texWidth, texHeight);
            }
        }
    }

    private static void region(GuiGraphics ctx, ResourceLocation texture, int x, int y, int width, int height, int u, int v, int texWidth, int texHeight) {
        if (width > 0 && height > 0) {
            blit(ctx, texture, x, y, width, height, u, v, width, height, texWidth, texHeight);
        }
    }

    private static Optional<GuiSpriteScaling> scalingFor(ResourceLocation texture) {
        registerListener();
        return SCALING.computeIfAbsent(texture, AnimatedTextures::loadScaling);
    }

    private static Optional<GuiSpriteScaling> loadScaling(ResourceLocation texture) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(resource.get().metadata().getSection(GuiMetadataSection.TYPE).map(GuiMetadataSection::scaling).orElse(GuiSpriteScaling.DEFAULT));
        } catch (IOException | RuntimeException e) {
            KevsAttributesPanel.LOGGER.warn("Failed to read scaling for {}", texture, e);
            return Optional.of(GuiSpriteScaling.DEFAULT);
        }
    }

    public static void blit(GuiGraphics ctx, ResourceLocation texture, int x, int y, int width, int height) {
        blit(ctx, texture, x, y, width, height, 0, 0, width, height, width, height);
    }

    public static void blit(GuiGraphics ctx, ResourceLocation texture, int x, int y, int width, int height, int u, int v, int regionWidth, int regionHeight, int texWidth, int texHeight) {
        Optional<Animation> found = animationFor(texture);
        if (found.isEmpty()) {
            ctx.blit(texture, x, y, width, height, u, v, regionWidth, regionHeight, texWidth, texHeight);
            return;
        }

        Animation animation = found.get();
        long position = frameTick % animation.totalTime();
        for (int i = 0; i < animation.frames().size(); i++) {
            Frame frame = animation.frames().get(i);
            if (position >= frame.time()) {
                position -= frame.time();
                continue;
            }

            if (animation.blended().isPresent()) {
                Frame next = animation.frames().get((i + 1) % animation.frames().size());
                animation.blended().get().update(animation, frame, next, (int) position);
                ctx.blit(animation.id(), x, y, width, height, u, v, regionWidth, regionHeight, texWidth, texHeight);
            } else {
                ctx.blit(animation.id(), x, y, width, height, u + animation.column(frame.index()) * texWidth, v + animation.row(frame.index()) * texHeight,
                        regionWidth, regionHeight, texWidth * animation.columns(), texHeight * animation.rows());
            }
            return;
        }
    }

    private static Optional<Animation> animationFor(ResourceLocation texture) {
        registerListener();
        return CACHE.computeIfAbsent(texture, AnimatedTextures::load);
    }

    private static void registerListener() {
        if (listenerRegistered) {
            return;
        }
        if (Minecraft.getInstance().getResourceManager() instanceof ReloadableResourceManager manager) {
            manager.registerReloadListener((ResourceManagerReloadListener) resources -> clear());
        }
        listenerRegistered = true;
    }

    private static void clear() {
        CACHE.values().forEach(found -> found.ifPresent(AnimatedTextures::close));
        CACHE.clear();
        SCALING.clear();
    }

    private static void close(Animation animation) {
        animation.blended().ifPresent(Blended::close);
        Minecraft.getInstance().getTextureManager().release(animation.id());
    }

    private static Optional<Animation> load(ResourceLocation texture) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isEmpty()) {
            return Optional.empty();
        }

        try {
            Optional<AnimationMetadataSection> section = resource.get().metadata().getSection(AnimationMetadataSection.SERIALIZER);
            if (section.isEmpty()) {
                return Optional.empty();
            }
            try (InputStream stream = resource.get().open()) {
                return build(texture, section.get(), NativeImage.read(stream));
            }
        } catch (IOException | RuntimeException e) {
            KevsAttributesPanel.LOGGER.warn("Failed to read animation for {}", texture, e);
            return Optional.empty();
        }
    }

    private static Optional<Animation> build(ResourceLocation texture, AnimationMetadataSection section, NativeImage image) {
        FrameSize size = section.calculateFrameSize(image.getWidth(), image.getHeight());
        if (size.width() <= 0 || size.height() <= 0 || image.getWidth() % size.width() != 0 || image.getHeight() % size.height() != 0) {
            KevsAttributesPanel.LOGGER.warn("Animation frame size {}x{} does not fit {} ({}x{})", size.width(), size.height(), texture, image.getWidth(), image.getHeight());
            image.close();
            return Optional.empty();
        }

        int columns = image.getWidth() / size.width();
        int rows = image.getHeight() / size.height();
        int frameCount = columns * rows;

        List<Frame> frames = new ArrayList<>();
        section.forEachFrame((index, time) -> {
            if (index >= 0 && index < frameCount && time > 0) {
                frames.add(new Frame(index, time));
            } else {
                KevsAttributesPanel.LOGGER.warn("Skipping invalid animation frame {} (time {}) in {}", index, time, texture);
            }
        });
        if (frames.isEmpty()) {
            for (int index = 0; index < frameCount; index++) {
                frames.add(new Frame(index, section.getDefaultFrameTime()));
            }
        }

        int totalTime = frames.stream().mapToInt(Frame::time).sum();
        if (totalTime <= 0) {
            KevsAttributesPanel.LOGGER.warn("Animation in {} has no playable frames", texture);
            image.close();
            return Optional.empty();
        }

        DynamicTexture own;
        Optional<Blended> blended = Optional.empty();
        if (section.isInterpolatedFrames()) {
            own = new DynamicTexture(size.width(), size.height(), false);
            blended = Optional.of(new Blended(image, own));
        } else {
            own = new DynamicTexture(image);
        }
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(KevsAttributesPanel.MOD_ID, OWN_PREFIX + texture.getNamespace() + PATH_SEPARATOR + texture.getPath());
        Minecraft.getInstance().getTextureManager().register(id, own);
        return Optional.of(new Animation(id, columns, rows, size.width(), size.height(), List.copyOf(frames), totalTime, blended));
    }

    private record Frame(int index, int time) {
    }

    private record Animation(ResourceLocation id, int columns, int rows, int frameWidth, int frameHeight, List<Frame> frames, int totalTime, Optional<Blended> blended) {
        int column(int index) {
            return index % columns;
        }

        int row(int index) {
            return index / columns;
        }
    }

    private static final class Blended {
        private final NativeImage source;
        private final DynamicTexture texture;
        private int shownFrom = -1;
        private int shownTo = -1;
        private int shownStep = -1;

        Blended(NativeImage source, DynamicTexture texture) {
            this.source = source;
            this.texture = texture;
        }

        void update(Animation animation, Frame from, Frame to, int step) {
            if (from.index() == shownFrom && to.index() == shownTo && step == shownStep) {
                return;
            }
            shownFrom = from.index();
            shownTo = to.index();
            shownStep = step;
            NativeImage target = texture.getPixels();
            int fromX = animation.column(from.index()) * animation.frameWidth();
            int fromY = animation.row(from.index()) * animation.frameHeight();
            int toX = animation.column(to.index()) * animation.frameWidth();
            int toY = animation.row(to.index()) * animation.frameHeight();
            for (int y = 0; y < animation.frameHeight(); y++) {
                for (int x = 0; x < animation.frameWidth(); x++) {
                    target.setPixelRGBA(x, y, mix(source.getPixelRGBA(fromX + x, fromY + y), source.getPixelRGBA(toX + x, toY + y), step, from.time()));
                }
            }
            texture.upload();
        }

        private static int mix(int from, int to, int step, int length) {
            int mixed = 0;
            for (int shift = 0; shift < PIXEL_BITS; shift += CHANNEL_BITS) {
                int a = (from >>> shift) & CHANNEL_MASK;
                int b = (to >>> shift) & CHANNEL_MASK;
                mixed |= ((a * (length - step) + b * step) / length) << shift;
            }
            return mixed;
        }

        void close() {
            source.close();
        }
    }
}
