package com.port.nexusclient;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Only respond to an explicit address to Nexus near the start of a transcript. */
final class NexusWakeWords {
    private static final Pattern CALL=Pattern.compile(
        "^(?:(?:ei|oi|ola|alo|o|oh|opa|fala|escuta|entao|cara|hey)[\\s,.!?:;]+){0,3}"
       +"(?:nexus|nexo|nexos)(?:\\s+(?:aeterna|aterna))?(?=$|[\\s,.!?:;])");

    private NexusWakeWords() {}

    static boolean addressed(String raw) {
        if(raw==null || raw.isBlank())return false;
        String normalized=Normalizer.normalize(raw,Normalizer.Form.NFD)
            .replaceAll("\\p{M}","").toLowerCase(Locale.ROOT)
            .replaceFirst("^[\\s,.!?:;]+","").trim();
        return CALL.matcher(normalized).find();
    }
}
