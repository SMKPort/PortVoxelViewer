package com.port.metamorph;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Local-only voxel attachment renderer.
 * It never changes player hitboxes, attributes or the multiplayer server.
 */
public final class MorphRenderer {
    public static void render(RenderPlayerEvent.Post event){
        Minecraft mc=Minecraft.getInstance();
        Player p=event.getEntity();
        if(mc.player==null||!mc.player.getUUID().equals(p.getUUID()))return;
        MorphState.Data state=MorphState.current();
        float active=MorphState.activation();
        if(active<=.001f)return;
        PoseStack stack=event.getPoseStack();
        MultiBufferSource buffer=event.getMultiBufferSource();
        int light=event.getPackedLight();
        BlockRenderDispatcher blocks=mc.getBlockRenderer();

        stack.pushPose();
        // RenderPlayerEvent is given the current player renderer pose stack.
        // Keep every part local to the player and never touch world block state.
        if(state.wings)renderWings(stack,buffer,blocks,light,state,active,event.getPartialTick());
        if(state.nexus)renderArm(stack,buffer,blocks,light,state,active,event.getPartialTick());
        if(state.horns)renderHorns(stack,buffer,blocks,light,state,active);
        if(state.cubes!=null){
            for(MorphState.Cube c:state.cubes){
                if(c==null)continue;
                block(stack,buffer,blocks,light,palette(c.material),
                    c.x,c.y,c.z,c.sx*active,c.sy*active,c.sz*active);
            }
        }
        stack.popPose();
    }

    private static void renderWings(PoseStack stack,MultiBufferSource buffer,
                                    BlockRenderDispatcher blocks,int light,
                                    MorphState.Data cfg,float active,float partial){
        float t=(MorphState.tickCount()+partial)/20f;
        double spread=cfg.wingScale*active;
        for(int sign:new int[]{-1,1}){
            double flap=Math.sin(t*cfg.wingSpeed)*cfg.wingMotion;
            stack.pushPose();
            stack.translate(sign*.23,1.40,.21);
            stack.mulPose(Axis.ZP.rotationDegrees((float)(sign*(14+flap))));
            for(int i=0;i<5;i++){
                double x=sign*(.17+i*.155)*spread;
                double y=(i*.10-.08)*spread;
                double length=(.24+(4-i)*.08)*spread;
                block(stack,buffer,blocks,light,palette(cfg.material),
                    x,y,.04,.16*spread,length,.12*spread);
                if(i<4)block(stack,buffer,blocks,light,Blocks.IRON_BLOCK.defaultBlockState(),
                    x,y+.10*spread,.025,.07*spread,.14*spread,.08*spread);
            }
            stack.popPose();
        }
    }

    private static void renderArm(PoseStack stack,MultiBufferSource buffer,
                                  BlockRenderDispatcher blocks,int light,
                                  MorphState.Data cfg,float active,float partial){
        double s=cfg.armScale*active;
        double pulse=.02*Math.sin((MorphState.tickCount()+partial)*.17);
        BlockState primary=palette(cfg.material);
        for(int i=0;i<4;i++){
            double y=1.36-i*.16;
            block(stack,buffer,blocks,light,primary,-.38,y,-.02,.21*s,.15*s,.25*s);
            block(stack,buffer,blocks,light,Blocks.IRON_BLOCK.defaultBlockState(),
                -.50,y-.02,.01+pulse,.06*s,.08*s,.17*s);
        }
        block(stack,buffer,blocks,light,Blocks.REDSTONE_BLOCK.defaultBlockState(),
            -.40,1.10,.12+pulse,.09*s,.09*s,.08*s);
    }

    private static void renderHorns(PoseStack stack,MultiBufferSource buffer,
                                    BlockRenderDispatcher blocks,int light,
                                    MorphState.Data cfg,float active){
        for(int dir:new int[]{-1,1}){
            BlockState b=palette(cfg.material);
            block(stack,buffer,blocks,light,b,dir*.21,1.90,.0,.12*active,.19*active,.13*active);
            block(stack,buffer,blocks,light,b,dir*.25,2.09,-.03,.08*active,.15*active,.09*active);
        }
    }

    private static BlockState palette(String material){
        return switch(material==null?"":material){
            case "iron"->Blocks.IRON_BLOCK.defaultBlockState();
            case "netherite"->Blocks.NETHERITE_BLOCK.defaultBlockState();
            case "amethyst"->Blocks.AMETHYST_BLOCK.defaultBlockState();
            case "redstone"->Blocks.REDSTONE_BLOCK.defaultBlockState();
            case "copper"->Blocks.COPPER_BLOCK.defaultBlockState();
            default->Blocks.GOLD_BLOCK.defaultBlockState();
        };
    }

    private static void block(PoseStack stack,MultiBufferSource buffer,
                              BlockRenderDispatcher renderer,int light,BlockState state,
                              double x,double y,double z,double width,double height,double depth){
        if(width<.001||height<.001||depth<.001)return;
        stack.pushPose();
        stack.translate(x-width/2,y-height/2,z-depth/2);
        stack.scale((float)width,(float)height,(float)depth);
        renderer.renderSingleBlock(state,stack,buffer,light,OverlayTexture.NO_OVERLAY);
        stack.popPose();
    }
    private MorphRenderer(){}
}
