package com.port.nexusclient;

import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALC11;
import org.lwjgl.openal.ALUtil;

import java.util.List;

/**
 * Enumerates OpenAL capture devices, without opening a microphone or
 * requesting recording permission. Only the VoiceCast capture thread opens
 * a microphone after the owner enables it.
 */
final class NexusMicrophones {
    private NexusMicrophones(){}

    static List<String> list(){
        try{
            List<String> names=ALUtil.getStringList(0L,ALC11.ALC_CAPTURE_DEVICE_SPECIFIER);
            return NexusMicrophoneChoices.unique(names);
        }catch(Throwable ex){
            // OpenAL may be missing or the OS may deny device enumeration.
            return List.of();
        }
    }

    static String systemDefault(){
        try{
            String name=ALC10.alcGetString(0L,ALC11.ALC_CAPTURE_DEFAULT_DEVICE_SPECIFIER);
            return name==null||name.isBlank()?"Dispositivo padrão":name;
        }catch(Throwable ex){
            return "Dispositivo padrão";
        }
    }

    static String displayName(String device){
        String cleaned=NexusMicrophoneChoices.normalize(device);
        return cleaned.isEmpty()?"Padrão do sistema ("+systemDefault()+")":cleaned;
    }

    static boolean isPresent(String device){
        String selected=NexusMicrophoneChoices.normalize(device);
        return selected.isEmpty()||list().contains(selected);
    }
}
