package net.pixeldreamstudios.attributepanel.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.HashMap;
import java.util.Map;

public class AttributeAnimationState {
    private final Map<ResourceLocation, ValueAnimation> valueAnimations = new HashMap<>();
    private final Map<ResourceLocation, GlowEffect> glowEffects = new HashMap<>();
    private long lastUpdateTime = System.currentTimeMillis();

    public void updateValue(Attribute attribute, double oldValue, double newValue) {
        ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(attribute);
        if (id == null) return;

        ValueAnimation anim = valueAnimations.get(id);
        if (anim == null) {
            anim = new ValueAnimation();
            valueAnimations.put(id, anim);
        }
        anim.startTransition(oldValue, newValue);

        GlowEffect glow = glowEffects.get(id);
        if (glow == null) {
            glow = new GlowEffect();
            glowEffects.put(id, glow);
        }
        glow.trigger();
    }

    public double getDisplayValue(Attribute attribute, double currentValue) {
        ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(attribute);
        if (id == null) return currentValue;

        ValueAnimation anim = valueAnimations.get(id);
        if (anim == null || !anim.isAnimating()) return currentValue;

        return anim.getCurrentValue();
    }

    public float getGlowIntensity(Attribute attribute) {
        ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(attribute);
        if (id == null) return 0f;

        GlowEffect glow = glowEffects.get(id);
        if (glow == null) return 0f;

        return glow.getIntensity();
    }

    public void stopGlow(Attribute attribute) {
        ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(attribute);
        if (id == null) return;

        GlowEffect glow = glowEffects.get(id);
        if (glow != null) {
            glow.stop();
        }
    }

    public void reset() {
        valueAnimations.clear();
        glowEffects.clear();
    }

    public void tick() {
        long now = System.currentTimeMillis();
        float deltaTime = (now - lastUpdateTime) / 1000f;
        lastUpdateTime = now;

        for (ValueAnimation anim : valueAnimations.values()) {
            anim.tick(deltaTime);
        }

        for (GlowEffect glow : glowEffects.values()) {
            glow.tick(deltaTime);
        }
    }

    public static class ValueAnimation {
        private double startValue;
        private double targetValue;
        private double currentValue;
        private float progress;
        private boolean animating;

        public void startTransition(double from, double to) {
            this.startValue = from;
            this.targetValue = to;
            this.currentValue = from;
            this.progress = 0f;
            this.animating = Math.abs(to - from) > 0.001;
        }

        public void tick(float deltaTime) {
            if (!animating) return;

            float speed = 5f;
            progress += deltaTime * speed;

            if (progress >= 1f) {
                progress = 1f;
                currentValue = targetValue;
                animating = false;
            } else {
                double diff = targetValue - startValue;
                int steps = Math.max(1, (int) Math.abs(diff));
                float stepProgress = progress * steps;
                int currentStep = (int) stepProgress;
                
                if (diff > 0) {
                    currentValue = startValue + currentStep;
                    if (currentValue > targetValue) currentValue = targetValue;
                } else {
                    currentValue = startValue - currentStep;
                    if (currentValue < targetValue) currentValue = targetValue;
                }
            }
        }

        public double getCurrentValue() {
            return currentValue;
        }

        public boolean isAnimating() {
            return animating;
        }
    }

    public static class GlowEffect {
        private float intensity;
        private float time;
        private boolean active;
        private static final float PULSE_DURATION = 1.5f;

        public void trigger() {
            this.intensity = 1f;
            this.time = 0f;
            this.active = true;
        }

        public void tick(float deltaTime) {
            if (!active) return;

            time += deltaTime;

            if (time >= PULSE_DURATION) {
                active = false;
                intensity = 0f;
                return;
            }

            float baseDecay = 1f - (time / PULSE_DURATION);
            float pulse = (float) Math.sin(time * 8f) * 0.3f + 0.7f;
            intensity = baseDecay * pulse;
        }

        public float getIntensity() {
            return active ? intensity : 0f;
        }

        public void stop() {
            this.active = false;
            this.intensity = 0f;
        }
    }
}
