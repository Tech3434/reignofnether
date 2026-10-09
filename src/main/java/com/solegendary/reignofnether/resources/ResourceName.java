package com.solegendary.reignofnether.resources;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

// actions that server can take to clients
public enum ResourceName implements StringRepresentable {
    FOOD,
    WOOD,
    ORE,
    EMERALD,
    NONE;

    @Override
    public String getSerializedName() {
        return this.toString().toLowerCase(Locale.ROOT);
    }

    public String langKey() {
        return "resources.reignofnether." + getSerializedName();
    }
}
