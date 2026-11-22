package com.port.nexusclient;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

@Mod(value = NexusClientMod.MOD_ID, dist = Dist.CLIENT)
public class NexusClientMod {
    public static final String MOD_ID = "nexusclient";

    private static final KeyMapping PRIVATE_KEY=new KeyMapping(
        "key.nexusclient.private",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_N,"key.categories.misc");
    private static final KeyMapping PERMISSIONS_KEY=new KeyMapping(
        "key.nexusclient.permissions",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_F8,"key.categories.misc");
    private static final KeyMapping HUD_KEY=new KeyMapping(
        "key.nexusclient.hud",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_F7,"key.categories.misc");
    private static final KeyMapping VOICE_CAST_KEY=new KeyMapping(
        "key.nexusclient.voicecast",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_F6,"key.categories.misc");
    private static final KeyMapping VISION_KEY=new KeyMapping(
        "key.nexusclient.vision",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_F9,"key.categories.misc");
    public NexusClientMod(IEventBus modBus) {
        modBus.addListener(this::onRegisterKeys);
        modBus.addListener(this::onRegisterHud);
        ClientConfig.load();
        // Paid Plasmo transcription has been retired. Keep archived memories only.
        NexusHearing.listen(false);
        NeoForge.EVENT_BUS.addListener(this::onTick);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        NeoForge.EVENT_BUS.addListener(this::onShutdown);
        NeoForge.EVENT_BUS.addListener(this::onAttackEntity);
    }

    private void onRegisterKeys(RegisterKeyMappingsEvent event){
        event.register(PRIVATE_KEY);
        event.register(PERMISSIONS_KEY);
        event.register(HUD_KEY);
        event.register(VOICE_CAST_KEY);
        event.register(VISION_KEY);
    }

    private void onRegisterHud(RegisterGuiLayersEvent event){
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(MOD_ID,"nexus_alerts"),
            (graphics,partialTick)->NexusCombatHud.render(graphics));
    }

    private void onShutdown(GameShuttingDownEvent event){
        NexusVoiceCasting.shutdown();
        NexusBuffManager.shutdown();
    }

    private void onAttackEntity(AttackEntityEvent event){
        NexusCombatMacro.onAttack(event);
        if(Minecraft.getInstance().player==event.getEntity())
            NexusBuffManager.markCombat();
    }

    private void onTick(ClientTickEvent.Post event) {
        ClientLink.tick();
        NexusVoiceCasting.tick();
        NexusAutoHeal.tick();
        NexusCombatMacro.tick();
        NexusBuffManager.tick();
        NexusHologram.tick();
        NexusVision.tick();
        Minecraft mc=Minecraft.getInstance();
        while(PRIVATE_KEY.consumeClick()){
            if(mc.player!=null&&mc.screen==null)
                mc.setScreen(new NexusPrivateScreen());
        }
        while(PERMISSIONS_KEY.consumeClick()){
            if(mc.player!=null&&mc.screen==null)
                mc.setScreen(new NexusPermissionsScreen());
        }
        while(HUD_KEY.consumeClick()){
            if(mc.player!=null&&mc.screen==null)
                mc.setScreen(new NexusHudEditorScreen(null));
        }
        while(VISION_KEY.consumeClick()){
            if(mc.player!=null&&mc.screen==null)
                mc.setScreen(new NexusVisionScreen(null));
        }
        while(VOICE_CAST_KEY.consumeClick()){
            if(mc.player!=null&&mc.screen==null){
                NexusVoiceCasting.setEnabled(!NexusVoiceCasting.enabled());
                notifyPlayer(NexusVoiceCasting.status());
            }
        }
    }

    private void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("nexusclient")
            .then(Commands.literal("status").executes(context -> {
                notifyPlayer(ClientLink.status());
                return 1;
            }))
            .then(Commands.literal("vision")
                .then(Commands.literal("menu").executes(ctx->{
                    Minecraft mc=Minecraft.getInstance();
                    if(mc.player!=null)mc.setScreen(new NexusVisionScreen(null));
                    return 1;
                }))
                .then(Commands.literal("on").executes(ctx->{
                    NexusVision.setEnabled(true);
                    notifyPlayer("Visão Arcana autorizada para capturas MANUAIS apenas.");return 1;
                }))
                .then(Commands.literal("off").executes(ctx->{
                    NexusVision.setEnabled(false);
                    notifyPlayer("Visão Arcana desativada.");return 1;
                }))
                .then(Commands.literal("status").executes(ctx->{
                    notifyPlayer(NexusVision.status());return 1;
                }))
                .then(Commands.literal("capture").executes(ctx->{
                    NexusVision.request("Descreva os elementos visíveis no mundo do Minecraft.");
                    return 1;
                })))
            .then(Commands.literal("hologram")
                .then(Commands.literal("menu").executes(ctx->{
                    Minecraft mc=Minecraft.getInstance();
                    if(mc.player!=null)mc.setScreen(new NexusVisionScreen(null));
                    return 1;
                }))
                .then(Commands.literal("on").executes(ctx->{
                    NexusHologram.setEnabled(true);notifyPlayer(NexusHologram.status());return 1;
                }))
                .then(Commands.literal("off").executes(ctx->{
                    NexusHologram.setEnabled(false);notifyPlayer(NexusHologram.status());return 1;
                }))
                .then(Commands.literal("status").executes(ctx->{
                    notifyPlayer(NexusHologram.status());return 1;
                }))
                .then(Commands.literal("model")
                    .then(Commands.literal("orb").executes(ctx->{
                        NexusHologram.setModel("orb");notifyPlayer(NexusHologram.status());return 1;
                    }))
                    .then(Commands.literal("runes").executes(ctx->{
                        NexusHologram.setModel("runes");notifyPlayer(NexusHologram.status());return 1;
                    }))
                    .then(Commands.literal("schematic").executes(ctx->{
                        NexusHologram.setModel("schematic");notifyPlayer(NexusHologram.status());return 1;
                    }))))
            .then(Commands.literal("menu").executes(context -> {
                Minecraft mc=Minecraft.getInstance();
                if(mc.player!=null)mc.setScreen(new NexusPermissionsScreen());
                return 1;
            }))
            .then(Commands.literal("permissions")
                .then(Commands.literal("menu").executes(context -> {
                    Minecraft mc=Minecraft.getInstance();
                    if(mc.player!=null)mc.setScreen(new NexusPermissionsScreen());
                    return 1;
                }))
                .then(Commands.literal("all").executes(context -> {
                    NexusPermissions.all();
                    return 1;
                }))
                .then(Commands.literal("off").executes(context -> {
                    NexusPermissions.off();
                    return 1;
                }))
                .then(Commands.literal("status").executes(context -> {
                    NexusPermissions.status();
                    return 1;
                })))
            .then(Commands.literal("hud")
                .then(Commands.literal("edit").executes(ctx->{
                    Minecraft mc=Minecraft.getInstance();
                    if(mc.player!=null)mc.setScreen(new NexusHudEditorScreen(null));
                    return 1;
                }))
                .then(Commands.literal("combat")
                    .then(Commands.literal("on").executes(ctx->{
                        NexusHudSettings.enabled("combat",true);
                        notifyPlayer("HUD de combate ativado.");return 1;
                    }))
                    .then(Commands.literal("off").executes(ctx->{
                        NexusHudSettings.enabled("combat",false);
                        notifyPlayer("HUD de combate ocultado.");return 1;
                    })))
                .then(Commands.literal("magic")
                    .then(Commands.literal("on").executes(ctx->{
                        NexusArcaneSensor.enabled(true);
                        NexusArcaneSensor.scanNearby(true);
                        NexusHudSettings.enabled("magic",true);
                        notifyPlayer("Leitura do Iron's Spells ativada no HUD.");return 1;
                    }))
                    .then(Commands.literal("off").executes(ctx->{
                        NexusHudSettings.enabled("magic",false);
                        NexusArcaneSensor.scanNearby(false);
                        NexusArcaneSensor.enabled(false);
                        notifyPlayer("Detecção de magia desligada.");return 1;
                    }))))
            .then(Commands.literal("memory")
                .then(Commands.literal("open").executes(ctx->{
                    Minecraft mc=Minecraft.getInstance();
                    if(mc.player!=null)mc.setScreen(new NexusMemoryScreen(null));
                    return 1;
                }))
                .then(Commands.literal("list").executes(ctx->{
                    var notes=NexusOwnerMemories.list();
                    notifyPlayer("Memórias manuais: "+notes.size()+"/200. Use F8 > MEMÓRIAS para editar.");
                    for(var note:notes.stream().limit(8).toList())
                        notifyPlayer("#"+note.id+" ["+note.topic+"] "+note.text);
                    return 1;
                }))
                .then(Commands.literal("add")
                    .then(Commands.argument("topic",StringArgumentType.string())
                    .then(Commands.argument("text",StringArgumentType.greedyString()).executes(ctx->{
                        try{
                            var memory=NexusOwnerMemories.save(0,
                                StringArgumentType.getString(ctx,"topic"),
                                StringArgumentType.getString(ctx,"text"));
                            notifyPlayer("Memória #"+memory.id+" salva: "+memory.topic);
                        }catch(Exception error){notifyPlayer("Memória: "+error.getMessage());}
                        return 1;
                    }))))
                .then(Commands.literal("find")
                    .then(Commands.argument("query",StringArgumentType.greedyString()).executes(ctx->{
                        String query=StringArgumentType.getString(ctx,"query");
                        var notes=NexusOwnerMemories.relevant(query,6);
                        if(notes.isEmpty())notifyPlayer("Nenhuma memória relacionada encontrada.");
                        else for(var note:notes)notifyPlayer("#"+note.id+" ["+note.topic+"] "+note.text);
                        return 1;
                    })))
                )
            .then(Commands.literal("pair")
                .then(Commands.argument("code", StringArgumentType.word()).executes(context -> {
                    ClientLink.pair(StringArgumentType.getString(context, "code"));
                    return 1;
                })))
            .then(Commands.literal("chat")
                .then(Commands.literal("on").executes(context -> {
                    ClientConfig.allowChat = true; ClientConfig.save();
                    notifyPlayer("Envio de mensagens autorizado. As mensagens sairão pela sua conta.");
                    return 1;
                }))
                .then(Commands.literal("off").executes(context -> {
                    ClientConfig.allowChat = false; ClientConfig.save();
                    notifyPlayer("Envio de mensagens REMOTAS bloqueado.");
                    return 1;
                })))
            .then(Commands.literal("telemetry")
                .then(Commands.literal("on").executes(context -> {
                    ClientConfig.allowTelemetry = true; ClientConfig.save();
                    ClientLink.resetTelemetryBaseline();
                    notifyPlayer("Telemetria de vida e efeitos ativada.");
                    return 1;
                }))
                .then(Commands.literal("off").executes(context -> {
                    ClientConfig.allowTelemetry = false; ClientConfig.save();
                    ClientLink.resetTelemetryBaseline();
                    notifyPlayer("Telemetria desativada.");
                    return 1;
                })))
            .then(Commands.literal("mind")
                .then(Commands.literal("status").executes(context -> {
                    notifyPlayer(NexusMind.status());return 1;
                }))
                .then(Commands.literal("on").executes(context -> {
                    ClientConfig.allowMind=true;ClientConfig.save();
                    NexusMind.resetSession();
                    notifyPlayer("Observação local autorizada. Memórias serão guardadas em config/nexus-consciousness.json.");
                    return 1;
                }))
                .then(Commands.literal("off").executes(context -> {
                    ClientConfig.allowMind=false;ClientConfig.allowAutonomy=false;
                    ClientConfig.save();NexusMind.resetSession();
                    notifyPlayer("Observação desativada. Memórias anteriores preservadas.");
                    return 1;
                }))
                .then(Commands.literal("memories").executes(context -> {
                    for(String memory:NexusMind.latestMemories())notifyPlayer(memory);
                    return 1;
                }))
                .then(Commands.literal("reset")
                    .then(Commands.literal("confirm").executes(context -> {
                        NexusMind.clearMemory();
                        notifyPlayer("Memória narrativa local reiniciada.");
                        return 1;
                    }))))
            .then(Commands.literal("autonomy")
                .then(Commands.literal("on").executes(context -> {
                    if(!ClientConfig.allowMind || !ClientConfig.allowChat) {
                        notifyPlayer("Antes, use /nexusclient mind on e /nexusclient chat on.");
                    } else {
                        ClientConfig.allowAutonomy=true;ClientConfig.save();
                        notifyPlayer("Autonomia autorizada: o Nexus poderá falar raramente via /l &6&l.");
                    }
                    return 1;
                }))
                .then(Commands.literal("off").executes(context -> {
                    ClientConfig.allowAutonomy=false;ClientConfig.save();
                    notifyPlayer("Mensagens espontâneas desativadas.");return 1;
                })))
            .then(Commands.literal("private")
                .then(Commands.literal("open").executes(ctx->{
                    Minecraft mc=Minecraft.getInstance();
                    if(mc.player!=null)mc.setScreen(new NexusPrivateScreen());
                    return 1;
                }))
                .then(Commands.literal("status").executes(ctx->{
                    notifyPlayer(NexusPrivateMind.status());return 1;
                }))
                .then(Commands.literal("on").executes(ctx->{
                    NexusPrivateMind.enable(true);
                    notifyPlayer("Canal neural privado habilitado. Pressione N ou use /nexusclient private open.");
                    return 1;
                }))
                .then(Commands.literal("off").executes(ctx->{
                    NexusPrivateMind.enable(false);
                    notifyPlayer("Canal privado desligado. Histórico local preservado.");
                    return 1;
                }))
                .then(Commands.literal("memory")
                    .then(Commands.literal("on").executes(ctx->{
                        NexusPrivateMind.memory(true);
                        notifyPlayer("Memória do chat privado salva localmente em config/nexus-private-mind.json. Arquivo não criptografado.");
                        return 1;
                    }))
                    .then(Commands.literal("off").executes(ctx->{
                        NexusPrivateMind.memory(false);
                        notifyPlayer("Memória privada desativada, histórico anterior apagado.");
                        return 1;
                    })))
                .then(Commands.literal("proactive")
                    .then(Commands.literal("on").executes(ctx->{
                        try{
                            NexusPrivateMind.setProactive(true);
                            notifyPlayer("Iniciativa privada autorizada nesta sessão.");
                        }catch(Exception e){notifyPlayer(e.getMessage());}
                        return 1;
                    }))
                    .then(Commands.literal("off").executes(ctx->{
                        NexusPrivateMind.setProactive(false);
                        notifyPlayer("Iniciativa privada desativada.");
                        return 1;
                    })))
                .then(Commands.literal("freedom")
                    .then(Commands.literal("on").executes(ctx->{
                        NexusPrivateMind.setFreedom(true);
                        notifyPlayer("Personalidade independente RP ativada.");
                        return 1;
                    }))
                    .then(Commands.literal("off").executes(ctx->{
                        NexusPrivateMind.setFreedom(false);
                        notifyPlayer("Personalidade cooperativa RP ativada.");
                        return 1;
                    })))
                .then(Commands.literal("clear")
                    .then(Commands.literal("confirm").executes(ctx->{
                        NexusPrivateMind.clear();
                        notifyPlayer("Conversas privadas locais apagadas.");
                        return 1;
                    })))
            )
            .then(Commands.literal("social")
                .then(Commands.literal("status").executes(ctx -> {
                    notifyPlayer(NexusSocial.status());return 1;}))
                .then(Commands.literal("diary").executes(ctx -> {
                    for(String note:NexusSocial.lastDiary())notifyPlayer(note);
                    return 1;}))
                .then(Commands.literal("share")
                    .then(Commands.literal("on").executes(ctx -> {
                        NexusSocial.setting("share",true);ClientLink.sendHearingState();
                        notifyPlayer("Diário liberado no seu painel privado.");return 1;}))
                    .then(Commands.literal("off").executes(ctx -> {
                        NexusSocial.setting("share",false);ClientLink.sendHearingState();
                        notifyPlayer("Diário novamente privado no PC.");return 1;})))
                .then(Commands.literal("protection")
                    .then(Commands.literal("on").executes(ctx -> {
                        NexusSocial.setting("protection",true);ClientLink.sendHearingState();
                        notifyPlayer("Proteção contextual habilitada.");return 1;}))
                    .then(Commands.literal("off").executes(ctx -> {
                        NexusSocial.setting("protection",false);ClientLink.sendHearingState();
                        notifyPlayer("Proteção contextual desabilitada.");return 1;})))
                .then(Commands.literal("forget").then(Commands.literal("confirm").executes(ctx -> {
                    NexusSocial.clear();ClientLink.sendHearingState();
                    notifyPlayer("Memórias sociais apagadas. Histórico legado de voz permanece preservado no PC.");
                    return 1;}))))
            .then(Commands.literal("macro")
                .then(Commands.literal("menu").executes(ctx->{
                    Minecraft mc=Minecraft.getInstance();
                    if(mc.player!=null)mc.setScreen(new NexusMacroScreen(null));
                    return 1;
                }))
                .then(Commands.literal("status").executes(ctx->{
                    notifyPlayer(NexusCombatMacro.status());return 1;
                }))
                .then(Commands.literal("on").executes(ctx->{
                    try{
                        NexusCombatMacro.setEnabled(true);
                        notifyPlayer(NexusCombatMacro.status());
                    }catch(Exception ex){notifyPlayer(ex.getMessage());}
                    return 1;
                }))
                .then(Commands.literal("off").executes(ctx->{
                    NexusCombatMacro.setEnabled(false);
                    notifyPlayer(NexusCombatMacro.status());return 1;
                }))
                .then(Commands.literal("hits")
                    .then(Commands.argument("count",
                        com.mojang.brigadier.arguments.IntegerArgumentType.integer(2,6))
                    .executes(ctx->{
                        int count=com.mojang.brigadier.arguments.IntegerArgumentType
                            .getInteger(ctx,"count");
                        NexusCombatMacro.setHits(count);
                        notifyPlayer("Macro: "+count+" golpes no mesmo alvo.");return 1;
                    })))
                .then(Commands.literal("window")
                    .then(Commands.argument("seconds",
                        com.mojang.brigadier.arguments.IntegerArgumentType.integer(2,10))
                    .executes(ctx->{
                        int seconds=com.mojang.brigadier.arguments.IntegerArgumentType
                            .getInteger(ctx,"seconds");
                        NexusCombatMacro.setWindow(seconds);
                        notifyPlayer("Macro: janela de "+seconds+" segundos.");return 1;
                    })))
                .then(Commands.literal("spell")
                    .then(Commands.argument("id",StringArgumentType.word())
                    .executes(ctx->{
                        try{
                            NexusCombatMacro.setSpell(StringArgumentType.getString(ctx,"id"));
                            notifyPlayer("Feitiço da macro: "+NexusCombatMacro.spell());
                        }catch(Exception ex){notifyPlayer(ex.getMessage());}
                        return 1;
                    }))))
            .then(Commands.literal("buffs")
                .then(Commands.literal("menu").executes(ctx->{
                    Minecraft mc=Minecraft.getInstance();
                    if(mc.player!=null)mc.setScreen(new NexusBuffScreen(null));
                    return 1;
                }))
                .then(Commands.literal("status").executes(ctx->{
                    notifyPlayer((NexusBuffManager.enabled()?"Buffs automáticos ON":"Buffs automáticos OFF")+
                        " | "+NexusBuffManager.status());return 1;
                }))
                .then(Commands.literal("list").executes(ctx->{
                    var items=NexusBuffManager.entries();
                    notifyPlayer("Buffs autorizados: "+items.size()+"/8.");
                    for(var e:items)
                        notifyPlayer("#"+e.id+" ["+e.mode+"] "+e.name+" ("+e.itemId+")");
                    return 1;
                }))
                .then(Commands.literal("add")
                    .then(Commands.literal("combat").executes(ctx->{
                        try{var e=NexusBuffManager.registerHeld(NexusBuffManager.COMBAT);
                            notifyPlayer("Buff "+e.name+" cadastrado para combate.");}
                        catch(Exception e){notifyPlayer(e.getMessage());}
                        return 1;
                    }))
                    .then(Commands.literal("manual").executes(ctx->{
                        try{var e=NexusBuffManager.registerHeld(NexusBuffManager.MANUAL);
                            notifyPlayer("Buff "+e.name+" cadastrado para uso manual.");}
                        catch(Exception e){notifyPlayer(e.getMessage());}
                        return 1;
                    })))
                .then(Commands.literal("on").executes(ctx->{
                    NexusBuffManager.setEnabled(true);
                    notifyPlayer("Buffs automáticos ON. Só itens cadastrados serão usados.");
                    return 1;
                }))
                .then(Commands.literal("off").executes(ctx->{
                    NexusBuffManager.setEnabled(false);
                    notifyPlayer("Buffs automáticos OFF.");return 1;
                }))
                .then(Commands.literal("use")
                    .then(Commands.argument("id",
                        com.mojang.brigadier.arguments.LongArgumentType.longArg(1))
                    .executes(ctx->{
                        try{
                            long id=com.mojang.brigadier.arguments.LongArgumentType.getLong(ctx,"id");
                            NexusBuffManager.useNow(id);
                            notifyPlayer(NexusBuffManager.status());
                        }catch(Exception e){notifyPlayer(e.getMessage());}
                        return 1;
                    }))))
            .then(Commands.literal("autoheal")
                .then(Commands.literal("status").executes(ctx->{
                    notifyPlayer("Cura automática: "+(NexusAutoHeal.enabled()?"ON":"OFF")+
                        " | Gatilho "+NexusAutoHeal.threshold()+"% da vida");
                    notifyPlayer(NexusAutoHeal.lastOutcome());
                    return 1;
                }))
                .then(Commands.literal("on").executes(ctx->{
                    try{
                        NexusAutoHeal.setEnabled(true);
                        notifyPlayer("Cura automática ON: abaixo de "+
                            NexusAutoHeal.threshold()+"% o Nexus tentará Heal.");
                    }catch(Exception ex){notifyPlayer(ex.getMessage());}
                    return 1;
                }))
                .then(Commands.literal("off").executes(ctx->{
                    NexusAutoHeal.setEnabled(false);
                    notifyPlayer("Cura automática OFF.");
                    return 1;
                }))
                .then(Commands.literal("threshold")
                    .then(Commands.argument("percent",com.mojang.brigadier.arguments.IntegerArgumentType.integer(10,75))
                        .executes(ctx->{
                            int value=com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx,"percent");
                            try{
                                NexusAutoHeal.setThreshold(value);
                                notifyPlayer("Cura automática: "+value+"% da vida máxima.");
                            }catch(Exception ex){notifyPlayer(ex.getMessage());}
                            return 1;
                        }))))
            .then(Commands.literal("voicecast")
                .then(Commands.literal("microphone")
                    .executes(ctx->{
                        Minecraft mc=Minecraft.getInstance();
                        if(mc.player!=null)mc.setScreen(new NexusMicrophoneScreen(null));
                        return 1;
                    })
                    .then(Commands.literal("list").executes(ctx->{
                        var devices=NexusMicrophones.list();
                        notifyPlayer("Microfone selecionado: "+NexusMicrophones.displayName(
                            NexusVoiceCasting.selectedMicrophone()));
                        notifyPlayer("Padrão: "+NexusMicrophones.systemDefault());
                        if(devices.isEmpty())notifyPlayer(
                            "Nenhum dispositivo enumerado. Verifique o microfone padrão do sistema.");
                        for(int i=0;i<devices.size();i++)
                            notifyPlayer((i+1)+": "+devices.get(i));
                        return 1;
                    }))
                    .then(Commands.literal("default").executes(ctx->{
                        NexusVoiceCasting.setMicrophone("");
                        notifyPlayer("Microfone padrão do sistema selecionado.");
                        return 1;
                    }))
                    .then(Commands.literal("select")
                        .then(Commands.argument("name",StringArgumentType.greedyString())
                            .executes(ctx->{
                                try{
                                    NexusVoiceCasting.setMicrophone(StringArgumentType.getString(ctx,"name"));
                                    notifyPlayer("Microfone: "+NexusMicrophones.displayName(
                                        NexusVoiceCasting.selectedMicrophone()));
                                }catch(Exception ex){notifyPlayer("Microfone: "+ex.getMessage());}
                                return 1;
                            }))))
                .then(Commands.literal("status").executes(ctx->{
                    notifyPlayer(NexusVoiceCasting.enabled()?"Conjuração de voz: ON":"Conjuração de voz: OFF");
                    notifyPlayer("Microfone selecionado: "+NexusMicrophones.displayName(
                        NexusVoiceCasting.selectedMicrophone())+
                        " | Em uso: "+(NexusVoiceCasting.capturingMicrophone().isEmpty()?
                            "nenhum":NexusVoiceCasting.capturingMicrophone()));
                    notifyPlayer("Idioma: "+NexusVoiceCasting.language()+
                        " | "+NexusVoiceCasting.status());
                    if(!NexusVoiceCasting.lastOutcome().isBlank())
                        notifyPlayer("Última ação: "+NexusVoiceCasting.lastOutcome());
                    return 1;
                }))
                .then(Commands.literal("on").executes(ctx->{
                    NexusVoiceCasting.setEnabled(true);
                    notifyPlayer(NexusVoiceCasting.status());return 1;
                }))
                .then(Commands.literal("off").executes(ctx->{
                    NexusVoiceCasting.setEnabled(false);
                    notifyPlayer(NexusVoiceCasting.status());return 1;
                }))
                .then(Commands.literal("diagnose").executes(ctx->{
                    notifyPlayer("VOICECAST: "+(NexusVoiceCasting.enabled()?"ATIVO":"DESLIGADO"));
                    notifyPlayer(NexusVoiceCasting.diagnostics());
                    notifyPlayer(NexusVoiceCasting.recognitionReport());
                    notifyPlayer("Estado: "+NexusVoiceCasting.status());
                    if(!NexusVoiceCasting.spellError().isBlank())
                        notifyPlayer("Falha na lista de magias: "+NexusVoiceCasting.spellError());
                    if(!NexusVoiceCasting.lastOutcome().isBlank())
                        notifyPlayer("Última solicitação: "+NexusVoiceCasting.lastOutcome());
                    return 1;
                }))
                .then(Commands.literal("check")
                    .then(Commands.argument("phrase",StringArgumentType.greedyString())
                    .executes(ctx->{
                        String phrase=StringArgumentType.getString(ctx,"phrase");
                        notifyPlayer(NexusVoiceCasting.checkPhrase(phrase));
                        return 1;
                    })))
                .then(Commands.literal("spells").executes(ctx->{
                    var player=Minecraft.getInstance().player;
                    if(player==null)return 0;
                    var spells=NexusVoiceCasting.equippedSpells(player);
                    notifyPlayer("Magias disponíveis para voz: "+spells.size());
                    for(var spell:spells.stream().limit(12).toList())
                        notifyPlayer("Slot "+spell.slot()+" = "+spell.id());
                    return 1;
                }))
                .then(Commands.literal("model")
                    .then(Commands.literal("install").executes(ctx->{
                        if(NexusVoiceModels.install(NexusVoiceCasting.language()))
                            notifyPlayer("Download solicitado do Vosk "+NexusVoiceCasting.language().toUpperCase()+
                                ". Progresso no /nexusclient voicecast status.");
                        else notifyPlayer(NexusVoiceModels.status());
                        return 1;
                    }))
                    .then(Commands.literal("status").executes(ctx->{
                        notifyPlayer("Modelo: "+NexusVoiceModels.status());return 1;
                    }))
                    .then(Commands.literal("pt").executes(ctx->{
                        NexusVoiceCasting.setLanguage("pt");
                        notifyPlayer("Português selecionado. Instale modelo com voicecast model install.");
                        return 1;
                    }))
                    .then(Commands.literal("en").executes(ctx->{
                        NexusVoiceCasting.setLanguage("en");
                        notifyPlayer("Inglês selecionado. Instale modelo com voicecast model install.");
                        return 1;
                    })))
            )
            .then(Commands.literal("voice")
                .then(Commands.literal("status").executes(ctx -> {
                    notifyPlayer(NexusSpeaker.status());return 1;}))
                .then(Commands.literal("on").executes(ctx -> {
                    NexusSpeaker.enable(true);ClientLink.sendHearingState();
                    notifyPlayer("Voz do Nexus autorizada somente no seu computador.");return 1;}))
                .then(Commands.literal("off").executes(ctx -> {
                    NexusSpeaker.enable(false);ClientLink.sendHearingState();
                    notifyPlayer("Voz local do Nexus desativada.");return 1;})))
            .then(Commands.literal("unpair").executes(context -> {
                // Always disable every capability, not only chat and telemetry.
                // The old command left Mind/Arcane/private permissions enabled.
                NexusPermissions.off();
                NexusSpeaker.stop();
                NexusPrivateMind.resetSession();
                ClientConfig.token="";ClientConfig.save();
                ClientLink.reset();
                notifyPlayer("Nexus desconectado. Permissões revogadas; memórias preservadas.");
                return 1;
            }))
        );
    }

    static void notifyPlayer(String text) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal("§6[ATERNA] §f" + text), false);
        }
    }
}
