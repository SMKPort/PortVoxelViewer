package com.port.audiobooster;

import android.content.SharedPreferences;

/**
 * Curvas digitais conservadoras para fones Xiaomi Buds e Redmi Buds.
 * A resposta real depende do modelo, das ponteiras e da vedacao.
 */
final class HeadphonePresets {
    static final String[] NAMES={
        "Xiaomi Buds Equilibrado","Graves Suaves","Voz Cristalina",
        "Funk nos Fones","Podcast","Neutro"
    };
    private static final int[][] SHAPES={
        {1,0,-1,1,1},  // equilibrado
        {2,1,-1,0,0},  // grave suave
        {-2,-1,1,2,1}, // voz
        {2,1,-1,1,1},  // funk moderado
        {-3,-1,1,2,0}, // fala
        {0,0,0,0,0}
    };
    static final int[] BASS={5,10,0,8,0,0};
    static int index(String s){
        for(int i=0;i<NAMES.length;i++)if(NAMES[i].equals(s))return i;
        return 0;
    }
    static void apply(SharedPreferences p,String requested){
        int n=index(requested);
        SharedPreferences.Editor ed=p.edit().putString("audio_mode","headphones")
            .putString("headphone_preset",NAMES[n])
            .putString("preset",NAMES[n])
            .putInt("bass",BASS[n]).putInt("bass_sub",0).putInt("bass_punch",0)
            .putInt("bass_impact",0).putInt("gain",0).putInt("space",0)
            .putBoolean("headphone_guard",true).putBoolean("safe_gain",true);
        for(int i=0;i<5;i++)ed.putInt("band_"+i,SHAPES[n][i]);
        for(int mode:new int[]{10,31}){
            int[] f=AudioProfiles.freqs(mode);
            for(int i=0;i<f.length;i++)ed.putInt("eq_"+mode+"_"+i,(int)Math.round(interpolate(SHAPES[n],f[i])));
        }
        ed.apply();
    }
    private static double interpolate(int[] curve,int hz){
        int[] f=AudioProfiles.FREQ5;
        if(hz<=f[0])return curve[0];
        if(hz>=f[f.length-1])return curve[curve.length-1];
        for(int i=0;i<f.length-1;i++)if(hz<=f[i+1]){
            double ratio=Math.log((double)hz/f[i])/Math.log((double)f[i+1]/f[i]);
            return curve[i]+(curve[i+1]-curve[i])*ratio;
        }
        return 0;
    }
}
