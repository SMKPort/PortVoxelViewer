package com.port.nexusclient;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Pure, testable logic. Never decides whether a server permits teleporting. */
final class NexusTravelPolicy {
    record Destination(UUID id, String name, String dimension, int x, int y, int z) {}
    record Match(Destination destination, String error) {
        boolean found() { return destination != null; }
    }
    record VoiceIntent(String action, String destination) {}
    private NexusTravelPolicy() {}

    static String normalize(String source) {
        if (source == null) return "";
        return Normalizer.normalize(source, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }

    static Match resolve(String query, Collection<Destination> known, Map<String, UUID> aliases) {
        String needle = normalize(query);
        if (needle.isEmpty()) return new Match(null, "Informe um destino.");
        if (known == null) return new Match(null, "Waystones ainda não disponíveis.");
        UUID alias = aliases == null ? null : aliases.get(needle);
        if (alias != null) {
            for (Destination destination : known)
                if (destination.id().equals(alias)) return new Match(destination, "");
            return new Match(null, "Favorito não está disponível neste mundo.");
        }
        List<Destination> exact = known.stream()
            .filter(it -> needle.equals(normalize(it.name()))).toList();
        if (exact.size() == 1) return new Match(exact.getFirst(), "");
        if (exact.size() > 1) return new Match(null, "Há Waystones com nomes iguais. Cadastre um favorito pelo UUID.");
        List<Destination> partial = known.stream()
            .filter(it -> normalize(it.name()).contains(needle)).toList();
        if (partial.size() == 1) return new Match(partial.getFirst(), "");
        if (partial.size() > 1) return new Match(null, "Destino ambíguo. Use mais palavras.");
        return new Match(null, "Waystone não encontrada na lista recebida do servidor.");
    }

    static boolean maySelect(boolean confirmed, boolean notExpired, boolean sameWorld,
                             boolean correctScreen, boolean memberOfMenu) {
        return confirmed && notExpired && sameWorld && correctScreen && memberOfMenu;
    }

    /** Exact, finite grammar. No natural-language model can call travel APIs. */
    static List<String> voicePhrases(Collection<String> destinationLabels) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String wake : List.of("nexus", "nexo")) {
            out.add(wake + " confirmar viagem");
            out.add(wake + " cancelar viagem");
            int count = 0;
            if (destinationLabels != null) {
                for (String label : destinationLabels) {
                    String normalized = normalize(label);
                    if (normalized.length() < 2 || normalized.length() > 48) continue;
                    if (count++ >= 40) break;
                    out.add(wake + " me leve para " + normalized);
                    out.add(wake + " viajar para " + normalized);
                }
            }
        }
        return List.copyOf(out);
    }

    static VoiceIntent interpretVoice(String spoken, Collection<String> vocabulary) {
        String phrase = normalize(spoken);
        if (vocabulary == null || !vocabulary.contains(phrase)) return null;
        if (phrase.endsWith(" confirmar viagem"))
            return new VoiceIntent("confirm", "");
        if (phrase.endsWith(" cancelar viagem"))
            return new VoiceIntent("cancel", "");
        for (String prefix : List.of("nexus me leve para ", "nexo me leve para ",
                "nexus viajar para ", "nexo viajar para ")) {
            if (phrase.startsWith(prefix))
                return new VoiceIntent("request", phrase.substring(prefix.length()));
        }
        return null;
    }

    static List<Destination> ordered(Collection<Destination> source) {
        if (source == null) return List.of();
        List<Destination> result = new ArrayList<>(source);
        result.sort(Comparator.comparing((Destination d) -> normalize(d.name()))
            .thenComparing(d -> d.id().toString()));
        return List.copyOf(result);
    }
}
