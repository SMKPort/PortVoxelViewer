package com.port.audiobooster;

import android.content.SharedPreferences;
import org.json.JSONObject;
import org.json.JSONException;

/** Serialização local de configurações musicais. Não exporta permissões nem dispositivos identificáveis. */
final class ProfileIO {
    private static final String[] SETTINGS={"bass","space","gain","bass_sub","bass_punch","bass_impact"};
    static String exportJson(SharedPreferences prefs)throws JSONException {
        JSONObject root=new JSONObject();
        root.put("format","port-audio-preset");root.put("version",3);
        root.put("name",prefs.getString("preset","Personalizado"));
        root.put("eq_mode",prefs.getInt("eq_mode",10));
        for(String key:SETTINGS)root.put(key,prefs.getInt(key,0));
        for(int i=0;i<5;i++)root.put("band_"+i,prefs.getInt("band_"+i,0));
        for(int mode:new int[]{10,31})for(int i=0;i<AudioProfiles.freqs(mode).length;i++)
            root.put("eq_"+mode+"_"+i,prefs.getInt("eq_"+mode+"_"+i,0));
        root.put("safe_gain",prefs.getBoolean("safe_gain",true));
        return root.toString(2);
    }
    static void importJson(SharedPreferences prefs,String json)throws JSONException {
        JSONObject obj=new JSONObject(json);
        if(!"port-audio-preset".equals(obj.optString("format"))||obj.optInt("version")!=3)
            throw new JSONException("Formato não suportado");
        SharedPreferences.Editor ed=prefs.edit();
        ed.putString("preset","Personalizado");
        int mode=obj.optInt("eq_mode",10);if(mode!=5&&mode!=10&&mode!=31)mode=10;
        ed.putInt("eq_mode",mode);
        for(String key:SETTINGS){
            int max=key.equals("gain")?6:key.equals("bass")||key.equals("space")?100:9;
            ed.putInt(key,AudioProfiles.clamp(obj.optInt(key,0),0,max));
        }
        for(int i=0;i<5;i++)
            ed.putInt("band_"+i,AudioProfiles.clamp(obj.optInt("band_"+i,0),-12,12));
        for(int n:new int[]{10,31})for(int i=0;i<AudioProfiles.freqs(n).length;i++)
            ed.putInt("eq_"+n+"_"+i,AudioProfiles.clamp(obj.optInt("eq_"+n+"_"+i,0),-12,12));
        ed.putBoolean("safe_gain",obj.optBoolean("safe_gain",true)).apply();
    }
}
