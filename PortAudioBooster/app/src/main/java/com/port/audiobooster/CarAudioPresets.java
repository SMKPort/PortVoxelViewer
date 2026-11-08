package com.port.audiobooster;

import android.content.SharedPreferences;

/** Presets dedicados ao carro. São regulagens digitais, não aferição do amplificador. */
final class CarAudioPresets {
    static final String[] NAMES={"Paredão Controlado","Pancada Limpa","Subgrave Suave","Voz Presente","Flat Seguro"};
    private static final int[][] SHAPES={
        {2,1,-1,2,1},
        {0,2,-1,2,0},
        {2,0,-1,1,0},
        {-2,-1,1,3,1},
        {0,0,0,0,0}
    };
    static final int[] BASS={12,10,8,0,0};
    static int find(String name){
        for(int i=0;i<NAMES.length;i++)if(NAMES[i].equals(name))return i;
        return 0;
    }
    static void apply(SharedPreferences p,String preset){
        int n=find(preset);
        SharedPreferences.Editor e=p.edit()
            .putString("audio_mode","car")
            .putString("car_preset",NAMES[n])
            .putString("preset",NAMES[n])
            .putInt("bass",BASS[n])
            .putInt("bass_sub",0)
            .putInt("bass_punch",0)
            .putInt("bass_impact",0)
            .putInt("gain",0)
            .putInt("space",0)
            .putBoolean("safe_gain",true);
        for(int i=0;i<5;i++)e.putInt("band_"+i,SHAPES[n][i]);
        for(int mode:new int[]{10,31}){
            int[] freq=AudioProfiles.freqs(mode);
            for(int i=0;i<freq.length;i++){
                int db=(int)Math.round(interpolate(SHAPES[n],freq[i]));
                e.putInt("eq_"+mode+"_"+i,db);
            }
        }
        e.apply();
    }
    private static double interpolate(int[] gain,int hz){
        int[] f=AudioProfiles.FREQ5;
        if(hz<=f[0])return gain[0];
        if(hz>=f[4])return gain[4];
        for(int i=0;i<4;i++)if(hz<=f[i+1]){
            double ratio=Math.log((double)hz/f[i])/Math.log((double)f[i+1]/f[i]);
            return gain[i]+ratio*(gain[i+1]-gain[i]);
        }
        return 0;
    }
    static String estimatedRisk(SharedPreferences p){
        if(p.getBoolean("compare_original",false))return "A • Sem efeitos";
        if(!p.getBoolean("enabled",false))return "DESLIGADO";
        if(p.getBoolean("car_guard",true))return "PROTEÇÃO PREVENTIVA ATIVA";
        double boost=p.getInt("gain",0);
        for(int i=0;i<AudioProfiles.freqs(p.getInt("eq_mode",10)).length;i++){
            String k=p.getInt("eq_mode",10)==5?"band_"+i:"eq_"+p.getInt("eq_mode",10)+"_"+i;
            boost=Math.max(boost,p.getInt(k,0)+p.getInt("gain",0));
        }
        int bass=p.getInt("bass",0);
        if(bass>=65 || boost>=7)return "RISCO ELEVADO POR AJUSTES";
        if(bass>=35 || boost>=4)return "RISCO MODERADO POR AJUSTES";
        return "GANHO DIGITAL MODERADO";
    }
}
