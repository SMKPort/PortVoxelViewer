package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Owner-approved item uses from hotbar or (with separate opt-in) main inventory.
 * The optional swap uses the VANILLA inventory menu ClickType.SWAP (not the
 * Hotbar Swapper keybind or a custom packet). Server validates the click/use.
 * On successful use the old hotbar stack is put back using the same click.
 */
final class NexusBuffManager {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE=FMLPaths.CONFIGDIR.get().resolve("nexus-buffs.json");
    static final String MANUAL="manual",COMBAT="combat";

    static final class Entry {
        long id;
        String itemId,name,signature,mode;
        boolean allowSingle=false;
        Entry(long id,String itemId,String name,String signature,String mode){
            this.id=id;this.itemId=itemId;this.name=name;
            this.signature=signature;this.mode=mode;
        }
    }
    private static final class Settings {
        int schema=1;
        long nextId=1;
        boolean enabled=false;
        boolean searchInventory=false; // separate owner opt-in, off by default
        List<Entry> entries=new ArrayList<>();
    }
    private static Settings settings;
    private static final Map<Long,Long> LAST_USE=new HashMap<>();
    private static long lastCombatAt=-1,lastAttemptAt=-1,sessionWorld=0;
    private static int usesThisCombat;
    private static long queuedManual=-1;
    private static Active active;
    private static InventorySwap swap;
    private enum SwapPhase { ARRIVAL, USING, RESTORE, VERIFY_RESTORED }
    private static final class InventorySwap {
        final long id;
        final int source,hotbar,originalSelected;
        final ItemStack displaced;
        final String fingerprint;
        final boolean manual;
        final long started;
        long phaseStarted;
        SwapPhase phase=SwapPhase.ARRIVAL;
        InventorySwap(Entry e,int src,int target,int selected,ItemStack prior,
                      boolean manual,long now){
            id=e.id;source=src;hotbar=target;originalSelected=selected;
            displaced=prior.copy();fingerprint=e.signature;
            this.manual=manual;started=now;phaseStarted=now;
        }
        void phase(SwapPhase next,long now){phase=next;phaseStarted=now;}
    }
    private static volatile String status="Nenhum buff ativo. Registre um item da hotbar.";

    private record Active(long entryId,int originalSlot,int buffSlot,
        String signature,long startedAt,boolean holdingUse,int ticks){}

    private NexusBuffManager(){}

    private static synchronized void load(){
        if(settings!=null)return;
        settings=new Settings();
        if(!Files.isRegularFile(FILE))return;
        try{
            Settings previous=JSON.fromJson(Files.readString(FILE,StandardCharsets.UTF_8),Settings.class);
            if(previous!=null&&previous.schema==1&&previous.entries!=null){
                previous.entries.removeIf(e->e==null||e.id<=0||
                    e.itemId==null||e.signature==null||e.name==null||
                    !e.itemId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"));
                if(previous.entries.size()>NexusBuffPolicy.MAX_REGISTERED)
                    previous.entries=new ArrayList<>(
                        previous.entries.subList(0,NexusBuffPolicy.MAX_REGISTERED));
                for(Entry entry:previous.entries){
                    entry.name=clean(entry.name,65);
                    entry.signature=clean(entry.signature,2048);
                    entry.mode=COMBAT.equals(entry.mode)?COMBAT:MANUAL;
                }
                previous.nextId=Math.max(previous.nextId,
                    previous.entries.stream().mapToLong(e->e.id).max().orElse(0)+1);
                settings=previous;
            }
        }catch(Exception ex){
            status="Arquivo de buffs inválido. O original foi preservado.";
        }
    }
    private static String clean(String raw,int limit){
        String val=String.valueOf(raw==null?"":raw).replaceAll("[\\p{Cntrl}§]"," ").trim();
        return val.substring(0,Math.min(val.length(),limit));
    }
    private static synchronized void save(){
        try{
            Files.createDirectories(FILE.getParent());
            Path temp=FILE.resolveSibling(FILE.getFileName()+".tmp");
            Files.writeString(temp,JSON.toJson(settings),StandardCharsets.UTF_8);
            try{Files.move(temp,FILE,StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException ex){
                Files.move(temp,FILE,StandardCopyOption.REPLACE_EXISTING);
            }
        }catch(Exception ex){throw new IllegalStateException("Não consegui salvar os buffs.",ex);}
    }

    static synchronized boolean enabled(){load();return settings.enabled;}
    static synchronized boolean inventoryEnabled(){load();return settings.searchInventory;}
    static synchronized void inventoryEnabled(boolean on){
        load();
        settings.searchInventory=on;
        save();
        status=on?"Busca de buffs no inventário ativada. Trocas temporárias permitidas.":
            "Busca no inventário desligada. Buffs restritos à hotbar.";
    }
    static synchronized void setEnabled(boolean on){
        load();settings.enabled=on;save();
        if(!on){queuedManual=-1;stopActive();status="Buffs automáticos desativados.";}
        else status="Buffs automáticos ativos. Só itens cadastrados serão usados.";
    }
    static synchronized List<Entry> entries(){
        load();return List.copyOf(settings.entries);
    }
    static String status(){return status;}
    static boolean busy(){return active!=null||swap!=null;}

    static synchronized Entry registerHeld(String mode){
        load();
        if(!MANUAL.equals(mode)&&!COMBAT.equals(mode))
            throw new IllegalArgumentException("Modo manual ou combat.");
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null)throw new IllegalStateException("Entre no mundo para registrar um item.");
        ItemStack stack=mc.player.getInventory().getSelected();
        if(stack.isEmpty())throw new IllegalArgumentException("Segure o item de buff na hotbar.");
        String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String signature=fingerprint(stack);
        if(signature.length()>2048)
            throw new IllegalArgumentException("Esse item possui dados grandes demais para cadastro.");
        for(Entry entry:settings.entries)
            if(entry.itemId.equals(id)&&entry.signature.equals(signature)){
                entry.mode=mode;save();status=entry.name+" atualizado para "+mode+".";
                return entry;
            }
        if(settings.entries.size()>=NexusBuffPolicy.MAX_REGISTERED)
            throw new IllegalStateException("Limite de oito buffs. Remova um antes.");
        Entry added=new Entry(settings.nextId++,id,
            clean(stack.getHoverName().getString(),65),signature,mode);
        settings.entries.add(added);save();
        status="Cadastrado "+added.name+
            (COMBAT.equals(mode)?" para combate.":" para comando manual.");
        return added;
    }
    static synchronized void setMode(long id,String mode){
        load();
        if(!MANUAL.equals(mode)&&!COMBAT.equals(mode))
            throw new IllegalArgumentException("Escolha manual ou combat.");
        Entry entry=find(id);
        entry.mode=mode;save();
        status=entry.name+" configurado para "+mode+".";
    }
    static synchronized void allowSingle(long id,boolean allow){
        Entry e=find(id);e.allowSingle=allow;save();
        status=allow?"Última unidade liberada explicitamente para "+e.name:
            "Última unidade protegida para "+e.name;
    }
    static synchronized void remove(long id){
        load();
        if(!settings.entries.removeIf(e->e.id==id))
            throw new IllegalArgumentException("Buff não cadastrado.");
        if(active!=null&&active.entryId==id)stopActive();
        if(swap!=null&&swap.id==id)status="Item removido. A troca pendente será restaurada.";
        LAST_USE.remove(id);save();status="Cadastro de buff removido.";
    }
    private static Entry find(long id){
        load();
        for(Entry e:settings.entries)if(e.id==id)return e;
        throw new IllegalArgumentException("Item não cadastrado.");
    }
    private static String fingerprint(ItemStack stack){
        // Potion contents discriminate red, blue and cyan potions even when
        // all of them have the same minecraft:potion registry ID.
        PotionContents potion=stack.get(DataComponents.POTION_CONTENTS);
        return potion!=null?"potion:"+potion:
            "components:"+stack.getComponents();
    }
    private static boolean matches(Entry e,ItemStack stack){
        return !stack.isEmpty()&&e.itemId.equals(
            BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())&&
            e.signature.equals(fingerprint(stack));
    }
    private static int slotOf(Entry e){
        var mc=Minecraft.getInstance();
        if(mc.player==null)return -1;
        for(int i=0;i<9;i++)
            if(matches(e,mc.player.getInventory().getItem(i)))return i;
        if(inventoryEnabled())
            for(int i=9;i<36;i++)
                if(matches(e,mc.player.getInventory().getItem(i)))return i;
        return -1;
    }
    private static boolean needsPotionEffect(ItemStack stack){
        PotionContents potion=stack.get(DataComponents.POTION_CONTENTS);
        if(potion==null||!potion.hasEffects())return true; // non-potion items need explicit registration
        var player=Minecraft.getInstance().player;
        if(player==null)return false;
        boolean beneficial=false,harmful=false,missing=false;
        for(MobEffectInstance desired:potion.getAllEffects()){
            if(!desired.getEffect().value().isBeneficial()){harmful=true;continue;}
            beneficial=true;
            MobEffectInstance existing=player.getEffect(desired.getEffect());
            if(existing==null||existing.getAmplifier()<desired.getAmplifier()||
                existing.getDuration()<300)missing=true;
        }
        return NexusBuffPolicy.effectNeedsRefresh(beneficial,harmful,missing);
    }

    static synchronized void markCombat(){
        load();
        if(!settings.enabled)return;
        long now=System.currentTimeMillis();
        if(lastCombatAt<0||now-lastCombatAt>NexusBuffPolicy.COMBAT_RESET_MS)
            usesThisCombat=0;
        lastCombatAt=now;
    }

    static synchronized void useNow(long id){
        find(id);queuedManual=id;
        status="Uso manual solicitado. Feche os menus para executar.";
    }

    static synchronized void tick(){
        load();
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||mc.gameMode==null){
            if(active!=null)stopActive();
            lastCombatAt=-1;usesThisCombat=0;queuedManual=-1;
            return;
        }
        long now=System.currentTimeMillis();
        if(swap!=null){updateInventorySwap(now);return;}
        if(active!=null){updateActive(now);return;}
        if(mc.screen!=null||!mc.isWindowActive()||mc.player.isSpectator()||
            !mc.player.isAlive()||mc.player.isUsingItem()||
            mc.options.keyUse.isDown()||NexusVoiceCasting.clientIsCasting())return;
        if(lastAttemptAt>=0&&now-lastAttemptAt<NexusBuffPolicy.MIN_DELAY_MS)return;

        if(queuedManual>0){
            long id=queuedManual;queuedManual=-1;
            Entry e;
            try{e=find(id);}catch(Exception ex){status=ex.getMessage();return;}
            int slot=slotOf(e);
            if(slot<0){status="Item "+e.name+" não encontrado na hotbar"+
                (inventoryEnabled()?" ou inventário.":". Ative BUSCAR NO INVENTÁRIO para permitir.");return;}
            long last=LAST_USE.getOrDefault(e.id,-1L);
            if(NexusBuffPolicy.manualEligible(true,false,now,last))
                useFromSlot(e,slot,true,now);
            return;
        }
        if(!settings.enabled||lastCombatAt<0||
            now-lastCombatAt>NexusBuffPolicy.COMBAT_ACTIVE_MS)return;
        if(mc.player.getHealth()/Math.max(1,mc.player.getMaxHealth())<.35f)return;
        for(Entry e:settings.entries){
            if(!COMBAT.equals(e.mode))continue;
            int slot=slotOf(e);
            if(slot<0)continue;
            ItemStack stack=mc.player.getInventory().getItem(slot);
            boolean last=stack.getCount()<=1&&!e.allowSingle;
            long used=LAST_USE.getOrDefault(e.id,-1L);
            if(NexusBuffPolicy.autoEligible(true,true,true,last,
                needsPotionEffect(stack),false,now,used,
                NexusBuffPolicy.AUTO_COOLDOWN_MS,usesThisCombat)){
                useFromSlot(e,slot,false,now);
                return;
            }
        }
    }

    /** A main-inventory item needs one confirmed vanilla swap before use. */
    private static void useFromSlot(Entry e,int slot,boolean manual,long now){
        if(slot<9){begin(e,slot,manual,now);return;}
        Minecraft mc=Minecraft.getInstance();
        if(!inventoryEnabled()||mc.player==null||mc.gameMode==null||
            mc.player.containerMenu!=mc.player.inventoryMenu)return;
        int selected=mc.player.getInventory().selected;
        if(!NexusBuffInventoryPolicy.mainInventoryIndex(slot))return;
        boolean[] emptySlots=new boolean[9];
        for(int i=0;i<9;i++)
            emptySlots[i]=mc.player.getInventory().getItem(i).isEmpty();
        int target=NexusBuffInventoryPolicy.reserveHotbar(emptySlots,selected);
        if(target<0)return;
        ItemStack old=mc.player.getInventory().getItem(target).copy();
        // Only the 9..35 main-inventory slots are eligible. No armor/offhand.
        try{
            swap=new InventorySwap(e,slot,target,selected,old,manual,now);
            // This uses the same Minecraft inventory-click protocol that ordinary
            // client-side hotbar switching mods use. Server retains authority.
            mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId,
                slot,target,ClickType.SWAP,mc.player);
            lastAttemptAt=now;
            status="Trocando temporariamente o item "+e.name+
                " do inventário para hotbar. Aguardando confirmação.";
        }catch(Throwable failure){
            swap=null;
            status="Falha ao trocar inventário. Nenhum buff utilizado.";
        }
    }

    private static boolean sameStack(ItemStack a,ItemStack b){
        if(a.isEmpty()||b.isEmpty())return a.isEmpty()&&b.isEmpty();
        return a.getCount()==b.getCount()&&a.getItem()==b.getItem()&&
            fingerprint(a).equals(fingerprint(b));
    }

    private static void updateInventorySwap(long now){
        InventorySwap op=swap;
        if(op==null)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||mc.gameMode==null){
            // Never emit a stale inventory packet after a disconnect.
            stopActive();swap=null;return;
        }
        ItemStack hotbar=mc.player.getInventory().getItem(op.hotbar);
        ItemStack source=mc.player.getInventory().getItem(op.source);
        if(op.phase==SwapPhase.ARRIVAL){
            if(now-op.phaseStarted<400)return; // allow server sync to settle
            if(sameStack(source,op.displaced)&&
                !hotbar.isEmpty()&&op.fingerprint.equals(fingerprint(hotbar))){
                if(mc.screen!=null||!mc.isWindowActive()||
                    mc.player.containerMenu!=mc.player.inventoryMenu){
                    op.phase(SwapPhase.RESTORE,now);return;
                }
                Entry registered=null;
                try{registered=find(op.id);}catch(Exception ignored){}
                if(registered!=null){
                    op.phase(SwapPhase.USING,now);
                    begin(registered,op.hotbar,op.manual,now);
                    if(active==null)op.phase(SwapPhase.RESTORE,now);
                }else op.phase(SwapPhase.RESTORE,now);
                return;
            }
            if(now-op.started>3_000){
                swap=null;
                status="Troca não confirmada. Confira slots e item manualmente; "
                    +"nenhum uso foi solicitado.";
            }
            return;
        }
        if(op.phase==SwapPhase.USING){
            if(active!=null){updateActive(now);return;}
            op.phase(SwapPhase.RESTORE,now);
            return;
        }
        if(op.phase==SwapPhase.RESTORE){
            // Wait for a long-duration item to finish and for client/server sync.
            if(now-op.phaseStarted<300||mc.player.isUsingItem())return;
            if(mc.screen!=null||!mc.isWindowActive()||
                mc.player.containerMenu!=mc.player.inventoryMenu||
                mc.player.getInventory().selected!=op.originalSelected){
                if(now-op.phaseStarted>10_000){
                    swap=null;
                    status="Restauração pausada. Volte aos slots normais manualmente; "
                        +"o Nexus não sobrescreveu suas alterações.";
                }
                return;
            }
            source=mc.player.getInventory().getItem(op.source);
            hotbar=mc.player.getInventory().getItem(op.hotbar);
            // Never overwrite a slot another mod/player changed during use.
            boolean expectedBuff=hotbar.isEmpty()||
                op.fingerprint.equals(fingerprint(hotbar));
            if(!NexusBuffInventoryPolicy.safeRestore(
                sameStack(source,op.displaced),expectedBuff,
                mc.player.getInventory().selected==op.originalSelected,
                mc.player.containerMenu==mc.player.inventoryMenu)){
                swap=null;
                status="Slots alterados durante o uso. Restauração automática "
                    +"cancelada para proteger o inventário.";
                return;
            }
            try{
                mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId,
                    op.source,op.hotbar,ClickType.SWAP,mc.player);
                op.phase(SwapPhase.VERIFY_RESTORED,now);
            }catch(Throwable failure){
                swap=null;
                status="Não consegui devolver a hotbar. Verifique os slots.";
            }
            return;
        }
        if(op.phase==SwapPhase.VERIFY_RESTORED){
            if(now-op.phaseStarted<400)return;
            hotbar=mc.player.getInventory().getItem(op.hotbar);
            if(sameStack(hotbar,op.displaced)){
                status="Buff utilizado e hotbar restaurada.";
            }else{
                status="Restauração não confirmada pelo cliente. Confira a hotbar.";
            }
            swap=null;
        }
    }

    private static void begin(Entry e,int slot,boolean manual,long now){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.gameMode==null)return;
        ItemStack stack=mc.player.getInventory().getItem(slot);
        if(!matches(e,stack))return;
        if(mc.player.getCooldowns().isOnCooldown(stack.getItem())){
            status="Item em cooldown: "+e.name;return;
        }
        int original=mc.player.getInventory().selected;
        if(original!=slot)mc.player.getInventory().selected=slot;
        try{
            InteractionResult result=mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);
            if(!result.consumesAction()){
                status="O item não respondeu ao uso normal: "+e.name;
                mc.player.getInventory().selected=original;
                return;
            }
            boolean holding=stack.getUseDuration(mc.player)>4;
            if(holding)mc.options.keyUse.setDown(true);
            active=new Active(e.id,original,slot,e.signature,now,holding,0);
            LAST_USE.put(e.id,now);
            lastAttemptAt=now;
            if(!manual)usesThisCombat++;
            status=(manual?"Uso manual":"Buff de combate")+
                " solicitado: "+e.name+".";
            NexusCombatHud.show("buff",status);
        }catch(Throwable ex){
            status="Falha ao usar "+e.name+" com o servidor.";
            if(mc.player!=null&&mc.player.getInventory().selected==slot)
                mc.player.getInventory().selected=original;
            if(mc.options.keyUse.isDown())mc.options.keyUse.setDown(false);
        }
    }
    private static void updateActive(long now){
        Minecraft mc=Minecraft.getInstance();
        Active op=active;
        if(op==null)return;
        if(mc.player==null||mc.gameMode==null){stopActive();return;}
        if(mc.player.getInventory().selected!=op.buffSlot()||
            mc.screen!=null||!mc.isWindowActive()||!mc.player.isAlive()||
            now-op.startedAt()>6500){
            stopActive();return;
        }
        ItemStack held=mc.player.getInventory().getSelected();
        if(!op.signature().equals(fingerprint(held))){
            stopActive();return;
        }
        int ticks=op.ticks()+1;
        active=new Active(op.entryId(),op.originalSlot(),op.buffSlot(),
            op.signature(),op.startedAt(),op.holdingUse(),ticks);
        if(op.holdingUse()){
            // Hold only while the item is actively consumed, then release
            // before the vanilla key handler could repeat another use.
            if(ticks>=2&&!mc.player.isUsingItem()){
                stopActive();return;
            }
            mc.options.keyUse.setDown(true);
        }else if(ticks>=2)stopActive();
    }

    private static void stopActive(){
        Active previous=active;active=null;
        if(previous==null)return;
        Minecraft mc=Minecraft.getInstance();
        if(previous.holdingUse()&&mc.options.keyUse.isDown())
            mc.options.keyUse.setDown(false);
        if(mc.player!=null&&mc.player.getInventory().selected==previous.buffSlot()){
            mc.player.getInventory().selected=previous.originalSlot();
        }
    }
    static synchronized void shutdown(){stopActive();queuedManual=-1;}
}
