package com.solegendary.reignofnether.research;

/**
 * What a research does. The behaviour lives in code; a {@link Research} instance only carries data.
 *
 * <p>UNLOCK has no effect of its own: units, buildings and abilities reference it through their own
 * required-research conditions. ATTRIBUTE_BOOST applies the research's attribute modifiers to the
 * owner's units (phase 3). EQUIP puts the research's items into the equipment slots of the owner's
 * matching units (phase 3).
 */
public enum ResearchType {
    ATTRIBUTE_BOOST,
    EQUIP,
    UNLOCK
}
