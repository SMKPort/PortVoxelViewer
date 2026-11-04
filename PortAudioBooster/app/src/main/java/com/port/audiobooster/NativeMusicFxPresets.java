package com.port.audiobooster;

import android.content.SharedPreferences;

/**
 * Referências para o MusicFX (não modificam nenhum controle do Android).
 * As faixas são as cinco bandas exibidas no aparelho de teste do usuário.
 */
final class NativeMusicFxPresets {
    static final int[] HZ = {60,230,910,4000,14000};
    static final String[] NAMES = {"Paredão","Grave Extremo","Funk","Voz Limpa","Rock","Neutro"};
    static final String[] DESCRIPTIONS = {
        "Subgrave forte com um pouco de presença na voz.",
        "Graves destacados. Use com cuidado para evitar distorção.",
        "Grave marcado e voz clara para batidas de funk.",
        "Mais inteligibilidade e menos excesso de grave.",
        "Ataque de guitarra, bateria e presença.",
        "Resposta plana para comparar a equalização."
    };
    static final int[][] BANDS = {
        {5,3,-2,2,1},
        {7,4,-3,0,0},
        {5,2,-2,3,2},
        {-2,-1,2,4,2},
        {3,2,-1,3,3},
        {0,0,0,0,0}
    };
    static final int[] BASS = {30,45,35,10,15,0};
    static final int[] SURROUND = {0,0,0,0,0,0};

    static int find(String name) {
        for(int i=0;i<NAMES.length;i++) if(NAMES[i].equals(name))return i;
        return 0;
    }
    static String selected(SharedPreferences p) {
        return p.getString("native_preset","Paredão");
    }
    static void select(SharedPreferences p,String name) {
        p.edit().putString("native_preset",NAMES[find(name)]).apply();
    }
    static String formatDb(int v) {return (v>0?"+":"")+v+" dB";}
}
