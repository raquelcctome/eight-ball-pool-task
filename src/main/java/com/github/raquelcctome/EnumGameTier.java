package com.github.raquelcctome;

public enum EnumGameTier {
    UNRANKED(1),
    BRONZE(2),
    SILVER(3),
    GOLD(4),
    DIAMOND(5),
    UNKNOWN(0);

    private final int tierCode;

    EnumGameTier(int tierCode) {
        this.tierCode = tierCode;
    }

    public static EnumGameTier getById(int id) {
        for(EnumGameTier tier : values()) {
            if(tier.tierCode == id)
                return tier;
        }
        return UNKNOWN;
    }
}
