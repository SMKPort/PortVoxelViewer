package com.port.nexusclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/**
 * Optional runtime adapter for Waystones 1.21.1. No direct dependency and no
 * registration of custom Waystones packets. Only sends Waystones' own packets.
 * The server exclusively controls access, costs, cooldown and teleport.
 */
final class NexusWaystoneBridge {
    private static final String API="net.blay09.mods.waystones.api.WaystonesAPI";
    private static final String WAYSTONE="net.blay09.mods.waystones.api.Waystone";
    private static final String MENU="net.blay09.mods.waystones.menu.WaystoneSelectionMenu";
    private static final String SCREEN="net.blay09.mods.waystones.client.gui.screen.WaystoneSelectionScreenBase";
    private static String lastError="";
    private NexusWaystoneBridge(){}

    static boolean present(){return ModList.get().isLoaded("waystones");}
    static String lastError(){return lastError;}

    /** Only waystones synced to the current client or offered in its open menu. */
    static List<NexusTravelPolicy.Destination> known() {
        Minecraft mc=Minecraft.getInstance();
        if (!present() || mc.player==null || mc.level==null) return List.of();
        LinkedHashMap<UUID,NexusTravelPolicy.Destination> result=new LinkedHashMap<>();
        try {
            Class<?> api=Class.forName(API);
            Object collection=api.getMethod("getActivatedWaystones",Player.class)
                .invoke(null,mc.player);
            if (collection instanceof Collection<?> entries)
                for(Object entry:entries) put(result,entry);
            if (selectionMenuOpen()) {
                Object menu=mc.player.containerMenu;
                Object offered=menu.getClass().getMethod("getWaystones").invoke(menu);
                if (offered instanceof Collection<?> entries)
                    for(Object entry:entries) put(result,entry);
            }
            lastError="";
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            lastError="Waystones: "+error.getClass().getSimpleName();
        }
        return NexusTravelPolicy.ordered(result.values());
    }

    private static void put(LinkedHashMap<UUID,NexusTravelPolicy.Destination> out,Object entry)
        throws ReflectiveOperationException {
        if (entry==null) return;
        Class<?> api=Class.forName(WAYSTONE);
        if (!api.isInstance(entry)) return;
        // The Waystones UI hides destinations hidden by player settings.
        try {
            Object hidden=entry.getClass().getMethod("isHidden").invoke(entry);
            if (Boolean.TRUE.equals(hidden)) return;
        } catch(NoSuchMethodException ignored) {
            // Base Waystone records do not expose hidden settings.
        }
        UUID id=(UUID)api.getMethod("getWaystoneUid").invoke(entry);
        if (id==null) return;
        Component name=(Component)api.getMethod("getEffectiveName").invoke(entry);
        BlockPos pos=(BlockPos)api.getMethod("getPos").invoke(entry);
        Object key=api.getMethod("getDimension").invoke(entry);
        String dimension=key instanceof ResourceKey<?> resourceKey
            ? resourceKey.location().toString() : "unknown";
        String label=name==null?"":name.getString();
        if (label.isBlank() || pos==null) return;
        out.put(id,new NexusTravelPolicy.Destination(id,label,dimension,
            pos.getX(),pos.getY(),pos.getZ()));
    }

    static boolean selectionMenuOpen(){
        Minecraft mc=Minecraft.getInstance();
        if (!present() || mc.player==null || mc.screen==null) return false;
        try {
            return Class.forName(MENU).isInstance(mc.player.containerMenu)
                && Class.forName(SCREEN).isInstance(mc.screen);
        } catch(ClassNotFoundException | LinkageError e){return false;}
    }

    static boolean menuContains(UUID id){
        if(!selectionMenuOpen() || id==null)return false;
        try{
            Object menu=Minecraft.getInstance().player.containerMenu;
            Object offered=menu.getClass().getMethod("getWaystones").invoke(menu);
            if(!(offered instanceof Collection<?> entries))return false;
            Class<?> api=Class.forName(WAYSTONE);
            for(Object entry:entries){
                if(entry!=null&&api.isInstance(entry) &&
                   id.equals(api.getMethod("getWaystoneUid").invoke(entry))){
                    try{
                        if(Boolean.TRUE.equals(entry.getClass().getMethod("isHidden").invoke(entry)))
                            return false;
                    }catch(NoSuchMethodException ignored){}
                    return true;
                }
            }
        }catch(ReflectiveOperationException | LinkageError | RuntimeException error){
            lastError="Menu Waystones indisponível: "+error.getClass().getSimpleName();
        }
        return false;
    }

    /** Exactly the packet used by Waystones' selection buttons. */
    static boolean chooseFromOpenMenu(UUID id){
        if(!menuContains(id))return false;
        try{
            Class<?> type=Class.forName("net.blay09.mods.waystones.network.message.SelectWaystoneMessage");
            Object payload=type.getConstructor(UUID.class).newInstance(id);
            PacketDistributor.sendToServer((CustomPacketPayload)payload);
            return true;
        }catch(ReflectiveOperationException | LinkageError | RuntimeException error){
            lastError="Seleção indisponível: "+error.getClass().getSimpleName();
            return false;
        }
    }

    /**
     * May open the normal server-provided destination GUI only when Waystones
     * explicitly configures inventory button mode ANY. Named and NEAREST modes
     * are never triggered, because they may teleport without showing a menu.
     */
    static boolean requestSafeInventoryMenu(){
        Minecraft mc=Minecraft.getInstance();
        if(!present()||mc.player==null||mc.screen!=null)return false;
        try{
            Class<?> config=Class.forName("net.blay09.mods.waystones.config.WaystonesConfig");
            Object settings=config.getMethod("getActive").invoke(null);
            if(settings==null)return false;
            Object mode=settings.getClass().getMethod("getInventoryButtonMode").invoke(settings);
            if(!Boolean.TRUE.equals(mode.getClass().getMethod("isReturnToAny").invoke(mode)))
                return false;
            if(!Boolean.TRUE.equals(mode.getClass().getMethod("isEnabled").invoke(mode)))
                return false;
            Class<?> packet=Class.forName("net.blay09.mods.waystones.network.message.InventoryButtonMessage");
            PacketDistributor.sendToServer((CustomPacketPayload)packet.getConstructor().newInstance());
            return true;
        }catch(ReflectiveOperationException | LinkageError | RuntimeException error){
            lastError="Menu do inventário não disponível: "+error.getClass().getSimpleName();
            return false;
        }
    }
}
