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
    }

    private void onAttackEntity(AttackEntityEvent event){
        NexusCombatMacro.onAttack(event);
    }

    private void onTick(ClientTickEvent.Post event) {
        ClientLink.tick();
        NexusVoiceCasting.tick();
        NexusAwakening.tick(); // opt-in HP guard before combat automation
        NexusAutoHeal.tick();
        NexusCombatMacro.tick();
        NexusBlessing.tick();
        NexusArcaneTravel.tick();
        NexusArcaneVision.tick();
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
        while(VOICE_CAST_KEY.consumeClick()){
            if(mc.player!=null&&mc.screen==null){
                NexusVoiceCasting.setEnabled(!NexusVoiceCasting.enabled());
                notifyPlayer(NexusVoiceCasting.status());
            }
        }
        while(VISION_KEY.consumeClick()){
            if(mc.player!=null&&mc.screen==null)
                mc.setScreen(new NexusVisionPanel(null));
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
                    if(mc.player!=null)mc.setScreen(new NexusVisionPanel(null));
                    return 1;
                }))
                .then(Commands.literal("status").executes(ctx->{
                    notifyPlayer(NexusArcaneVision.status());return 1;
                }))
                .then(Commands.literal("last").executes(ctx->{
                    notifyPlayer("Última Visão Arcana: "+NexusArcaneVision.lastResult());return 1;
                }))
                .then(Commands.literal("on").executes(ctx->{
                    try{
                        NexusArcaneVision.setEnabled(true);
                        notifyPlayer("Visão Arcana autorizada. F9 abre o painel; nada é capturado automaticamente.");
                    }catch(Exception ex){notifyPlayer(ex.getMessage());}
                    return 1;
                }))
                .then(Commands.literal("off").executes(ctx->{
                    NexusArcaneVision.setEnabled(false);
                    notifyPlayer("Visão Arcana desativada. Capturas pendentes canceladas.");return 1;
                }))
                .then(Commands.literal("focus")
                    .then(Commands.literal("general").executes(ctx->{
                        NexusArcaneVision.setFocus(NexusVisionModes.Focus.GERAL);
                        notifyPlayer(NexusArcaneVision.status());return 1;
                    }))
                    .then(Commands.literal("structures").executes(ctx->{
                        NexusArcaneVision.setFocus(NexusVisionModes.Focus.ESTRUTURAS);
                        notifyPlayer(NexusArcaneVision.status());return 1;
                    }))
                    .then(Commands.literal("danger").executes(ctx->{
                        NexusArcaneVision.setFocus(NexusVisionModes.Focus.PERIGOS);
                        notifyPlayer(NexusArcaneVision.status());return 1;
                    }))
                    .then(Commands.literal("items").executes(ctx->{
                        NexusArcaneVision.setFocus(NexusVisionModes.Focus.ITENS);
                        notifyPlayer(NexusArcaneVision.status());return 1;
                    })))
                .then(Commands.literal("zoom")
                    .then(Commands.literal("on").executes(ctx->{
                        NexusArcaneVision.setCenterCrop(true);
                        notifyPlayer("Visão Arcana: centro ampliado.");return 1;
                    }))
                    .then(Commands.literal("off").executes(ctx->{
                        NexusArcaneVision.setCenterCrop(false);
                        notifyPlayer("Visão Arcana: tela inteira.");return 1;
                    })))
                .then(Commands.literal("analyze").executes(ctx->{
                    NexusArcaneVision.capture("");return 1;
                }).then(Commands.argument("pergunta",StringArgumentType.greedyString())
                    .executes(ctx->{
                        NexusArcaneVision.capture(
                            StringArgumentType.getString(ctx,"pergunta"));return 1;
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
            .then(Commands.literal("travel")
                .then(Commands.literal("menu").executes(ctx -> {
                    Minecraft mc=Minecraft.getInstance();
                    if(mc.player!=null)mc.setScreen(new NexusTravelScreen(null));
                    return 1;
                }))
                .then(Commands.literal("status").executes(ctx -> {
                    notifyPlayer(NexusArcaneTravel.pendingStatus());
                    notifyPlayer(NexusArcaneTravel.status());
                    return 1;
                }))
                .then(Commands.literal("list").executes(ctx -> {
                    var destinations=NexusArcaneTravel.known();
                    notifyPlayer("Waystones ativadas/visíveis: "+destinations.size()
                        +". Abra /nexusclient travel menu para navegar.");
                    for(var target:destinations.stream().limit(16).toList())
                        notifyPlayer(target.name()+" ["+target.dimension()+"] "
                            +target.x()+" "+target.y()+" "+target.z());
                    return 1;
                }))
                .then(Commands.literal("favorites").executes(ctx -> {
                    var favorites=NexusArcaneTravel.favoritesSummary();
                    if(favorites.isEmpty())notifyPlayer("Nenhum favorito salvo neste mundo.");
                    else for(String item:favorites)notifyPlayer(item);
                    return 1;
                }))
                .then(Commands.literal("favorite")
                    .then(Commands.argument("alias",StringArgumentType.string())
                    .then(Commands.argument("destination",StringArgumentType.greedyString())
                        .executes(ctx -> {
                            notifyPlayer(NexusArcaneTravel.favorite(
                                StringArgumentType.getString(ctx,"alias"),
                                StringArgumentType.getString(ctx,"destination")));
                            return 1;
                        }))))
                .then(Commands.literal("remove")
                    .then(Commands.argument("alias",StringArgumentType.greedyString())
                        .executes(ctx -> {
                            notifyPlayer(NexusArcaneTravel.removeFavorite(
                                StringArgumentType.getString(ctx,"alias")));
                            return 1;
                        })))
                .then(Commands.literal("go")
                    .then(Commands.argument("destination",StringArgumentType.greedyString())
                        .executes(ctx -> {
                            notifyPlayer(NexusArcaneTravel.request(
                                StringArgumentType.getString(ctx,"destination")));
                            return 1;
                        })))
                .then(Commands.literal("confirm").executes(ctx -> {
                    notifyPlayer(NexusArcaneTravel.confirm());return 1;
                }))
                .then(Commands.literal("cancel").executes(ctx -> {
                    notifyPlayer(NexusArcaneTravel.cancel());return 1;
                })))
            .then(Commands.literal("awakening")
                .then(Commands.literal("status").executes(ctx->{
                    notifyPlayer(NexusAwakening.status());return 1;
                }))
                .then(Commands.literal("report").executes(ctx->{
                    NexusAwakening.report(false);return 1;
                }))
                .then(Commands.literal("protect")
                    .then(Commands.literal("on").executes(ctx->{
                        notifyPlayer(NexusAwakening.protection(true));return 1;
                    }))
                    .then(Commands.literal("off").executes(ctx->{
                        notifyPlayer(NexusAwakening.protection(false));return 1;
                    })))
                .then(Commands.literal("warnings")
                    .then(Commands.literal("on").executes(ctx->{
                        notifyPlayer(NexusAwakening.warnings(true));return 1;
                    }))
                    .then(Commands.literal("off").executes(ctx->{
                        notifyPlayer(NexusAwakening.warnings(false));return 1;
                    })))
                .then(Commands.literal("threshold")
                    .then(Commands.argument("percent",
                        com.mojang.brigadier.arguments.IntegerArgumentType.integer(15,60))
                        .executes(ctx->{
                            int amount=com.mojang.brigadier.arguments.IntegerArgumentType
                                .getInteger(ctx,"percent");
                            notifyPlayer(NexusAwakening.threshold(amount));return 1;
                        })))
                .then(Commands.literal("journal")
                    .then(Commands.literal("on").executes(ctx->{
                        notifyPlayer(NexusAwakening.journal(true));return 1;
                    }))
                    .then(Commands.literal("off").executes(ctx->{
                        notifyPlayer(NexusAwakening.journal(false));return 1;
                    }))
                    .then(Commands.literal("recent").executes(ctx->{
                        for(String item:NexusAwakening.journalEntries())notifyPlayer(item);
                        return 1;
                    }))
                    .then(Commands.literal("clear")
                        .then(Commands.literal("confirm").executes(ctx->{
                            notifyPlayer(NexusAwakening.clearJournal());return 1;
                        }))))
                .then(Commands.literal("recall").executes(ctx->{
                    NexusAwakening.recall("",false);return 1;
                }).then(Commands.argument("topic",StringArgumentType.greedyString())
                    .executes(ctx->{
                        NexusAwakening.recall(StringArgumentType.getString(ctx,"topic"),false);
                        return 1;
                    }))))
            .then(Commands.literal("situacao").executes(ctx->{
                NexusAwakening.report(false);return 1;
            }))
            .then(Commands.literal("memory")
                .then(Commands.literal("ai")
                    .then(Commands.literal("status").executes(ctx->{
                        notifyPlayer("Compartilhar memórias relevantes com Gemini: "+
                            (ClientConfig.allowGeminiMemory?"AUTORIZADO":"DESLIGADO"));
                        return 1;
                    }))
                    .then(Commands.literal("on").executes(ctx->{
                        if(ClientConfig.token.isBlank()||!ClientConfig.allowChat){
                            notifyPlayer("Pareie o Nexus e use /nexusclient chat on antes de autorizar Gemini.");
                            return 0;
                        }
                        ClientConfig.allowGeminiMemory=true;
                        ClientConfig.save();
                        notifyPlayer("Gemini poderá consultar até 6 memórias manuais relevantes por pergunta. "+
                            "Use /nexusclient memory ai off para revogar.");
                        return 1;
                    }))
                    .then(Commands.literal("off").executes(ctx->{
                        ClientConfig.allowGeminiMemory=false;
                        ClientConfig.save();
                        notifyPlayer("Acesso do Gemini às memórias manuais revogado. Registros preservados.");
                        return 1;
                    })))
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
                .then(Commands.literal("list").executes(ctx->{
                    var macros=NexusCombatMacro.entries();
                    if(macros.isEmpty())notifyPlayer("Nenhuma macro de combate cadastrada.");
                    for(var macro:macros)notifyPlayer(
                        (macro.enabled()?"[ON] ":"[OFF] ")+macro.spell()+
                        " | "+macro.hits()+" golpes | "+macro.windowSeconds()+
                        "s | intervalo "+macro.intervalSeconds()+"s | "+
                        String.join(" > ",macro.sequence()));
                    return 1;
                }))
                .then(Commands.literal("spells").executes(ctx->{
                    var spells=NexusCombatMacro.candidates();
                    if(spells.isEmpty())notifyPlayer("Nenhuma magia equipada detectada.");
                    else for(String id:spells.stream().limit(40).toList())
                        notifyPlayer("Equipada: "+id);
                    return 1;
                }))
                .then(Commands.literal("add")
                    .then(Commands.argument("id",StringArgumentType.word()).executes(ctx->{
                        try{notifyPlayer(NexusCombatMacro.addSpell(
                            StringArgumentType.getString(ctx,"id")));}
                        catch(Exception error){notifyPlayer(error.getMessage());}
                        return 1;
                    })))
                .then(Commands.literal("phrase")
                    .then(Commands.argument("text",StringArgumentType.greedyString())
                        .executes(ctx->{
                            notifyPlayer(NexusCombatMacro.setVoicePhrase(
                                StringArgumentType.getString(ctx,"text")));
                            return 1;
                        })))
                .then(Commands.literal("clear-phrase").executes(ctx->{
                    notifyPlayer(NexusCombatMacro.setVoicePhrase(""));
                    return 1;
                }))
                .then(Commands.literal("announce-start")
                    .then(Commands.argument("text",StringArgumentType.greedyString())
                        .executes(ctx->{
                            notifyPlayer(NexusCombatMacro.setAnnouncements(
                                StringArgumentType.getString(ctx,"text"),
                                NexusCombatMacro.finishPhrase(),
                                NexusCombatMacro.announceHud(),NexusCombatMacro.announceChat()));
                            return 1;
                        })))
                .then(Commands.literal("announce-finish")
                    .then(Commands.argument("text",StringArgumentType.greedyString())
                        .executes(ctx->{
                            notifyPlayer(NexusCombatMacro.setAnnouncements(
                                NexusCombatMacro.startPhrase(),
                                StringArgumentType.getString(ctx,"text"),
                                NexusCombatMacro.announceHud(),NexusCombatMacro.announceChat()));
                            return 1;
                        })))
                .then(Commands.literal("voicecheck")
                    .then(Commands.argument("text",StringArgumentType.greedyString())
                        .executes(ctx->{
                            String spoken=StringArgumentType.getString(ctx,"text");
                            String key=NexusMacroPhrasePolicy.interpret(spoken,
                                NexusCombatMacro.voicePhrases());
                            notifyPlayer(key==null?"Frase não identificada. Diga Nexus + frase cadastrada.":
                                "Frase reconhecida para macro "+key+" (sem executar).");
                            return 1;
                        })))
                .then(Commands.literal("sequence").executes(ctx->{
                    notifyPlayer("Sequência de "+NexusCombatMacro.spell()+": "+
                        String.join(" > ",NexusCombatMacro.sequence()));
                    notifyPlayer(NexusCombatMacro.queueStatus());
                    return 1;
                }))
                .then(Commands.literal("append")
                    .then(Commands.argument("id",StringArgumentType.word()).executes(ctx->{
                        try{notifyPlayer(NexusCombatMacro.addToSequence(
                            StringArgumentType.getString(ctx,"id")));}
                        catch(Exception error){notifyPlayer(error.getMessage());}
                        return 1;
                    })))
                .then(Commands.literal("remove-spell")
                    .then(Commands.argument("id",StringArgumentType.word()).executes(ctx->{
                        try{notifyPlayer(NexusCombatMacro.removeFromSequence(
                            StringArgumentType.getString(ctx,"id")));}
                        catch(Exception error){notifyPlayer(error.getMessage());}
                        return 1;
                    })))
                .then(Commands.literal("move-up")
                    .then(Commands.argument("id",StringArgumentType.word()).executes(ctx->{
                        try{notifyPlayer(NexusCombatMacro.moveInSequence(
                            StringArgumentType.getString(ctx,"id"),-1));}
                        catch(Exception error){notifyPlayer(error.getMessage());}
                        return 1;
                    })))
                .then(Commands.literal("move-down")
                    .then(Commands.argument("id",StringArgumentType.word()).executes(ctx->{
                        try{notifyPlayer(NexusCombatMacro.moveInSequence(
                            StringArgumentType.getString(ctx,"id"),1));}
                        catch(Exception error){notifyPlayer(error.getMessage());}
                        return 1;
                    })))
                .then(Commands.literal("select")
                    .then(Commands.argument("id",StringArgumentType.word()).executes(ctx->{
                        try{notifyPlayer(NexusCombatMacro.selectSpell(
                            StringArgumentType.getString(ctx,"id")));}
                        catch(Exception error){notifyPlayer(error.getMessage());}
                        return 1;
                    })))
                .then(Commands.literal("remove")
                    .then(Commands.argument("id",StringArgumentType.word()).executes(ctx->{
                        try{notifyPlayer(NexusCombatMacro.removeSpell(
                            StringArgumentType.getString(ctx,"id")));}
                        catch(Exception error){notifyPlayer(error.getMessage());}
                        return 1;
                    })))
                .then(Commands.literal("on").executes(ctx->{
                    try{
                        NexusCombatMacro.setSelectedEnabled(true);
                        notifyPlayer(NexusCombatMacro.status());
                    }catch(Exception ex){notifyPlayer(ex.getMessage());}
                    return 1;
                }))
                .then(Commands.literal("disable").executes(ctx->{
                    try{
                        NexusCombatMacro.setSelectedEnabled(false);
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
                        try{
                            int count=com.mojang.brigadier.arguments.IntegerArgumentType
                                .getInteger(ctx,"count");
                            NexusCombatMacro.setHits(count);
                            notifyPlayer("Macro selecionada: "+count+" golpes.");
                        }catch(Exception error){notifyPlayer(error.getMessage());}
                        return 1;
                    })))
                .then(Commands.literal("interval")
                    .then(Commands.argument("seconds",
                        com.mojang.brigadier.arguments.IntegerArgumentType.integer(2,30))
                    .executes(ctx->{
                        try{
                            int seconds=com.mojang.brigadier.arguments.IntegerArgumentType
                                .getInteger(ctx,"seconds");
                            NexusCombatMacro.setIntervalSeconds(seconds);
                            notifyPlayer(NexusCombatMacro.lastOutcome());
                        }catch(Exception error){notifyPlayer(error.getMessage());}
                        return 1;
                    })))
                .then(Commands.literal("window")
                    .then(Commands.argument("seconds",
                        com.mojang.brigadier.arguments.IntegerArgumentType.integer(2,10))
                    .executes(ctx->{
                        try{
                            int seconds=com.mojang.brigadier.arguments.IntegerArgumentType
                                .getInteger(ctx,"seconds");
                            NexusCombatMacro.setWindow(seconds);
                            notifyPlayer("Macro selecionada: "+seconds+" segundos.");
                        }catch(Exception error){notifyPlayer(error.getMessage());}
                        return 1;
                    })))
                .then(Commands.literal("spell")
                    .then(Commands.argument("id",StringArgumentType.word()).executes(ctx->{
                        try{
                            NexusCombatMacro.setSpell(StringArgumentType.getString(ctx,"id"));
                            notifyPlayer("Magia da macro selecionada: "+NexusCombatMacro.spell());
                        }catch(Exception ex){notifyPlayer(ex.getMessage());}
                        return 1;
                    }))))
            .then(Commands.literal("blessing")
                .then(Commands.literal("menu").executes(ctx->{
                    Minecraft mc=Minecraft.getInstance();
                    if(mc.player!=null)mc.setScreen(new NexusBlessingScreen(null));
                    return 1;
                }))
                .then(Commands.literal("status").executes(ctx->{
                    notifyPlayer(NexusBlessing.summary());
                    notifyPlayer(NexusBlessing.selectedRemaining());
                    return 1;
                }))
                .then(Commands.literal("list").executes(ctx->{
                    var rules=NexusBlessing.entries();
                    if(rules.isEmpty())notifyPlayer("Nenhuma magia de manutenção cadastrada.");
                    for(var rule:rules)notifyPlayer(
                        (rule.enabled()?"[ON] ":"[OFF] ")+rule.spellId()+
                        " | "+rule.thresholdSeconds()+"s | "+rule.effects().size()+" efeito(s)");
                    return 1;
                }))
                .then(Commands.literal("spells").executes(ctx->{
                    var options=NexusBlessing.candidates();
                    if(options.isEmpty())notifyPlayer("Nenhuma magia equipada detectada.");
                    else for(String id:options.stream().limit(40).toList())
                        notifyPlayer("Equipada: "+id);
                    return 1;
                }))
                .then(Commands.literal("add")
                    .then(Commands.argument("id",StringArgumentType.word()).executes(ctx->{
                        notifyPlayer(NexusBlessing.addSpell(
                            StringArgumentType.getString(ctx,"id")));
                        return 1;
                    })))
                .then(Commands.literal("select")
                    .then(Commands.argument("id",StringArgumentType.word()).executes(ctx->{
                        notifyPlayer(NexusBlessing.selectSpell(
                            StringArgumentType.getString(ctx,"id")));
                        return 1;
                    })))
                .then(Commands.literal("remove")
                    .then(Commands.argument("id",StringArgumentType.word()).executes(ctx->{
                        notifyPlayer(NexusBlessing.removeSpell(
                            StringArgumentType.getString(ctx,"id")));
                        return 1;
                    })))
                .then(Commands.literal("effects").executes(ctx->{
                    for(String line:NexusBlessing.effectsStatus())notifyPlayer(line);
                    return 1;
                }))
                .then(Commands.literal("learn").executes(ctx->{
                    notifyPlayer(NexusBlessing.learn());return 1;
                }))
                .then(Commands.literal("on").executes(ctx->{
                    notifyPlayer(NexusBlessing.setSelectedEnabled(true));return 1;
                }))
                .then(Commands.literal("disable").executes(ctx->{
                    notifyPlayer(NexusBlessing.setSelectedEnabled(false));return 1;
                }))
                .then(Commands.literal("off").executes(ctx->{
                    notifyPlayer(NexusBlessing.setEnabled(false));return 1;
                }))
                .then(Commands.literal("seconds")
                    .then(Commands.argument("value",
                        com.mojang.brigadier.arguments.IntegerArgumentType.integer(3,60))
                        .executes(ctx->{
                            int seconds=com.mojang.brigadier.arguments.IntegerArgumentType
                                .getInteger(ctx,"value");
                            notifyPlayer(NexusBlessing.setThreshold(seconds));return 1;
                        })))
                .then(Commands.literal("spell")
                    .then(Commands.argument("id",StringArgumentType.word()).executes(ctx->{
                        notifyPlayer(NexusBlessing.setSpell(
                            StringArgumentType.getString(ctx,"id")));
                        return 1;
                    })))
                .then(Commands.literal("effect")
                    .then(Commands.argument("id",StringArgumentType.word()).executes(ctx->{
                        notifyPlayer(NexusBlessing.trackEffect(
                            StringArgumentType.getString(ctx,"id")));
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
                        String requested=NexusVoiceModels.nextMissing(NexusVoiceCasting.language());
                        if(requested.isEmpty())notifyPlayer("Modelos PT e EN já estão instalados.");
                        else if(NexusVoiceModels.install(requested))
                            notifyPlayer("Download solicitado do Vosk "+requested.toUpperCase()
                                +". Instale o outro modelo para reconhecer PT+EN simultaneamente.");
                        else notifyPlayer(NexusVoiceModels.status());
                        return 1;
                    })
                    .then(Commands.literal("en").executes(ctx->{
                        notifyPlayer(NexusVoiceModels.install("en")?
                            "Instalação do modelo inglês solicitada.":NexusVoiceModels.status());
                        return 1;
                    }))
                    .then(Commands.literal("pt").executes(ctx->{
                        notifyPlayer(NexusVoiceModels.install("pt")?
                            "Instalação do modelo português solicitada.":NexusVoiceModels.status());
                        return 1;
                    })))
                    .then(Commands.literal("status").executes(ctx->{
                        notifyPlayer("Modelo: "+NexusVoiceModels.status());return 1;
                    }))
                    .then(Commands.literal("auto").executes(ctx->{
                        NexusVoiceCasting.setLanguage("auto");
                        notifyPlayer("Modo automático PT+EN selecionado. Instale os dois modelos "
                            +"em /nexusclient voicecast model install para usar os dois idiomas.");
                        return 1;
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
