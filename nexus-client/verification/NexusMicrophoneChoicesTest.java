package com.port.nexusclient;

import java.util.List;

public final class NexusMicrophoneChoicesTest {
    private static void check(boolean valid,String message){
        if(!valid)throw new AssertionError(message);
    }
    public static void main(String[] args){
        var discovered=List.of(
            "Microfone USB (HyperX)", "Microfone embutido", "Microfone USB (HyperX)", " ");
        var unique=NexusMicrophoneChoices.unique(discovered);
        check(unique.equals(List.of("Microfone USB (HyperX)","Microfone embutido")),
            "Duplicate and blank devices removed, names keep their order");
        var choices=NexusMicrophoneChoices.withDefault(discovered);
        check(choices.size()==3&&choices.get(0).isEmpty(),
            "Always offer system default separately from discovered devices");
        check(NexusMicrophoneChoices.available("",List.of()),
            "System default remains usable even when enumeration is unsupported");
        check(NexusMicrophoneChoices.available(null,unique),
            "Old config with no microphone saves as OS default");
        check(NexusMicrophoneChoices.available("Microfone USB (HyperX)",unique),
            "Exact selected microphone can be used");
        check(!NexusMicrophoneChoices.available("Microfone desconectado",unique),
            "A missing named microphone must never fall back silently");
        check(!NexusMicrophoneChoices.available("microfone usb (hyperx)",unique),
            "Device selection respects OS device names exactly");
        check(NexusMicrophoneChoices.normalize("  Microfone USB  ").equals("Microfone USB"),
            "Trim persisted microphone name");
        check(NexusMicrophoneChoices.normalize("Mic\nruim").equals("Mic ruim"),
            "Control characters must be removed from device names");
        check(NexusMicrophoneChoices.normalize("x".repeat(300)).length()==200,
            "Bound device name length in local configuration");
        check(NexusMicrophoneChoices.unique(null).isEmpty(),
            "Missing enumeration results must not crash");
        System.out.println("NexusMicrophoneChoicesTest OK");
    }
}
