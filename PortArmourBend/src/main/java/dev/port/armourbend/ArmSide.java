package dev.port.armourbend;

import java.util.Locale;

public enum ArmSide {
    RIGHT, LEFT;

    public static ArmSide fromChildName(String raw) {
        if (raw == null) return null;
        String name = raw.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (isNamedFor(name, "r", "right", "direito")) return RIGHT;
        if (isNamedFor(name, "l", "left", "esquerdo")) return LEFT;
        return null;
    }

    private static boolean isNamedFor(String name, String shortSide, String longSide, String portugueseSide) {
        String[] bases = {"forearm", "lowerarm", "lower_arm", "antebraco", "antebraço", "hand", "cotovelo", "elbow"};
        for (String base : bases) {
            if (name.equals(base + "_" + shortSide) || name.equals(base + "_" + longSide) || name.equals(base + "_" + portugueseSide)) return true;
            if (name.equals(base + "_" + shortSide + "2") || name.equals(base + "_" + longSide + "2")) return true;
            if (name.equals(base + "2_" + shortSide) || name.equals(base + "2_" + longSide)) return true;
        }
        if (name.equals("arm_" + shortSide + "2") || name.equals("arm_" + longSide + "2")) return true;
        return false;
    }
}
