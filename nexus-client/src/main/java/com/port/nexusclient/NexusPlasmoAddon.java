package com.port.nexusclient;

import su.plo.voice.api.addon.AddonInitializer;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.addon.InjectPlasmoVoice;
import su.plo.voice.api.client.PlasmoVoiceClient;
import su.plo.voice.api.client.event.audio.source.AudioSourceWriteEvent;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.proto.data.audio.source.PlayerSourceInfo;

/**
 * Optional Plasmo Voice 2.1.8 client addon.
 * Only samples pre-consented, individually allowlisted real player sources.
 */
@Addon(id="pv-addon-nexus-aterna",name="Nexus Aterna Hearing",version="1.7.0",authors={"Port"})
public final class NexusPlasmoAddon implements AddonInitializer {
    @InjectPlasmoVoice private PlasmoVoiceClient voiceClient;

    static void attach(){
        PlasmoVoiceClient.getAddonsLoader().load(new NexusPlasmoAddon());
    }

    @Override public void onAddonInitialize(){
        System.out.println("Nexus Hearing: Plasmo Voice addon carregado.");
        NexusHearing.setPlasmoReady(true);
    }
    @Override public void onAddonShutdown(){
        NexusHearing.setPlasmoReady(false);
    }
    @EventSubscribe
    public void onVoiceWrite(AudioSourceWriteEvent event){
        if(!NexusHearing.isListening())return;
        if(!(event.getSource().getSourceInfo() instanceof PlayerSourceInfo source))return;
        if(source.getPlayerInfo()==null)return;
        final String speaker=source.getPlayerInfo().getPlayerNick();
        if(!NexusHearing.isAllowed(speaker)||!event.getSource().canHear())return;
        var info=voiceClient.getServerInfo();
        if(info.isEmpty())return;
        boolean stereo=source.isStereo();
        int sampleRate=(int)info.get().getVoiceInfo().createFormat(stereo).getSampleRate();
        NexusHearing.capture(speaker,event.getSamples(),sampleRate,stereo?2:1);
    }
}
