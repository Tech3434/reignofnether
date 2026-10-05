package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;

import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Frustum.class)
public class FrustumMixin {

    // I have no idea why this is needed but without it the game freezes and gets stuck inside
    // this function forever a few seconds after activating orthoView
    @Inject(
            method = "offsetToFullyIncludeCameraCube",
            at = @At("HEAD"),
            cancellable = true
    )
    private void offsetToFullyIncludeCameraCube(int p_194442_, CallbackInfoReturnable<Frustum> cir) {
        if (OrthoviewClientEvents.isEnabled()) {
            cir.setReturnValue((Frustum) (Object) this);
        }
    }

    /*
     * The beacon/end-portal frustum exemption this mixin used to carry is gone with the API it
     * keyed on. 1.21.1 removed IBlockEntityExtension#getRenderBoundingBox (and with it
     * INFINITE_EXTENT_AABB), so no block entity reports a world-sized AABB to Frustum#isVisible
     * any more - block entities are culled per render section instead. The hook below therefore
     * could never fire, and reviving it would need a different injection point:
     * BlockEntityRenderer#shouldRender(T, Vec3).
     */
}