package com.port.audiobooster;

import android.content.SharedPreferences;

/** Presets e perfis de equalização em bandas virtuais. */
final class AudioProfiles {
    static final int[] FREQ5 = {60, 230, 910, 3600, 14000};
    static final int[] FREQ10 = {31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000};
    static final int[] FREQ31 = {20,25,31,40,50,63,80,100,125,160,200,250,315,400,500,630,800,1000,1250,1600,2000,2500,3150,4000,5000,6300,8000,10000,12500,16000,20000};
    static final String[] NAMES = {"Paredão","Grave Extremo","Voz Limpa","Funk","Grave Profundo","Voz Destacada","Rock","Eletrônica","Flat","Personalizado"};
    private static final int[][] SHAPES = {
        {6,7,-2,3,1},{9,9,-3,0,-1},{-4,-3,2,5,3},{7,6,-1,4,2},
        {8,7,-1,0,1},{-3,-2,2,5,2},{3,3,-2,4,3},{5,5,0,3,5},{0,0,0,0,0}
    };
    private static final int[] BASS = {70,90,0,72,82,12,35,55,0};
    private static final int[] SUB = {6,9,0,7,8,0,1,5,0};
    private static final int[] PUNCH = {5,8,0,6,6,0,3,4,0};
    private static final int[] IMPACT = {3,8,0,5,3,0,2,4,0};

    static int[] freqs(int mode) { return mode==31 ? FREQ31 : mode==10 ? FREQ10 : FREQ5; }
    static int clamp(int n, int min, int max) { return Math.max(min,Math.min(max,n)); }

    static void setPreset(SharedPreferences prefs,String name) {
        int p=0; for(int i=0;i<NAMES.length;i++) if(NAMES[i].equals(name)) p=i;
        SharedPreferences.Editor edit=prefs.edit().putString("preset",NAMES[p]);
        if(p==NAMES.length-1) {edit.apply(); return;}
        for(int i=0;i<5;i++) edit.putInt("band_"+i,SHAPES[p][i]);
        for(int mode:new int[]{10,31}) {
            int[] fq=freqs(mode);
            for(int i=0;i<fq.length;i++)
                edit.putInt("eq_"+mode+"_"+i,(int)Math.round(interpolate5(SHAPES[p],fq[i])));
        }
        edit.putInt("bass",BASS[p]).putInt("bass_sub",SUB[p]).putInt("bass_punch",PUNCH[p])
            .putInt("bass_impact",IMPACT[p]).apply();
    }
    private static double interpolate5(int[] levels,double hz) {
        if(hz<=FREQ5[0])return levels[0];
        if(hz>=FREQ5[4])return levels[4];
        for(int i=0;i<4;i++)if(hz<=FREQ5[i+1]) {
            double t=(Math.log(hz)-Math.log(FREQ5[i]))/(Math.log(FREQ5[i+1])-Math.log(FREQ5[i]));
            return levels[i]+(levels[i+1]-levels[i])*t;
        }
        return 0;
    }
    static double atHz(SharedPreferences prefs,double hz) {
        int mode=prefs.getInt("eq_mode",10);
        if(mode!=5&&mode!=10&&mode!=31)mode=10;
        int[] freq=freqs(mode);
        String prefix=mode==5?"band_":"eq_"+mode+"_";
        if(hz<=freq[0])return prefs.getInt(prefix+"0",0);
        if(hz>=freq[freq.length-1])return prefs.getInt(prefix+(freq.length-1),0);
        for(int i=0;i<freq.length-1;i++)if(hz<=freq[i+1]){
            double k=(Math.log(hz)-Math.log(freq[i]))/(Math.log(freq[i+1])-Math.log(freq[i]));
            return prefs.getInt(prefix+i,0)*(1-k)+prefs.getInt(prefix+(i+1),0)*k;
        }
        return 0;
    }
    static double bassDb(SharedPreferences prefs,double hz){
        double sub=prefs.getInt("bass_sub",0)*Math.exp(-0.5*Math.pow(Math.log(Math.max(hz,1)/45d)/0.50,2));
        double punch=prefs.getInt("bass_punch",0)*Math.exp(-0.5*Math.pow(Math.log(Math.max(hz,1)/95d)/0.34,2));
        double impact=prefs.getInt("bass_impact",0)*Math.exp(-0.5*Math.pow(Math.log(Math.max(hz,1)/155d)/0.32,2));
        return sub+punch+impact;
    }
}
