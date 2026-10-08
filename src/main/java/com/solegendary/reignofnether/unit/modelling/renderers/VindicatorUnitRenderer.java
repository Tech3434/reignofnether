package com.solegendary.reignofnether.unit.modelling.renderers;

import com.solegendary.reignofnether.unit.modelling.models.VillagerUnitModel;
import com.solegendary.reignofnether.unit.units.villagers.VindicatorUnit;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

// VindicatorUnit reuses the illager body model without any villager profession overlay.
@OnlyIn(Dist.CLIENT)
public class VindicatorUnitRenderer extends AbstractVillagerUnitRenderer<VindicatorUnit> {

    private static final ResourceLocation VINDICATOR_UNIT =
            ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/entities/vindicator_unit.png");

    public VindicatorUnitRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerUnitModel<>(context.bakeLayer(VillagerUnitModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(VindicatorUnit unit) {
        return VINDICATOR_UNIT;
    }
}
