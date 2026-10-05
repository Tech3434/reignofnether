package com.solegendary.reignofnether.unit.goals;

public class CreeperAttackUnitGoal extends AbstractMeleeAttackUnitGoal {
    private final CreeperUnit creeperUnit;

    public CreeperAttackUnitGoal(CreeperUnit creeperUnit, int attackInterval, boolean followingTargetEvenIfNotSeen) {
        super(creeperUnit, followingTargetEvenIfNotSeen);
        this.creeperUnit = creeperUnit;
    }

    public void tick() {
        if (creeperUnit.canExplodeOnTarget())
            creeperUnit.setSwellDir(1);
    }
}
