package net.pixeldreamstudios.attributepanel.client;

public enum DisplayMode {
    NONE,
    FRACTION_0_TO_1,
    BASE_100,
    MULTIPLIER_BASE_100,
    MULTIPLIER_BASE_1,
    MULTIPLIER_BASE_0;
    
    public boolean isPercent() {
        return this == FRACTION_0_TO_1 || this == BASE_100;
    }
    
    public boolean isMultiplier() {
        return this == MULTIPLIER_BASE_100 || this == MULTIPLIER_BASE_1 || this == MULTIPLIER_BASE_0;
    }
}
