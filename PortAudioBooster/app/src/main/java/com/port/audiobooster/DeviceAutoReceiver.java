package com.port.audiobooster;

import android.Manifest;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * No modo MusicFX, só seleciona referências salvas.
 * Não tenta manipular MusicFX, abrir Activity em background ou iniciar DSP global.
 */
public final class DeviceAutoReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent) {
        if(intent==null || !BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction())) return;
        SharedPreferences prefs=context.getSharedPreferences(GlobalAudioEngine.PREFS,Context.MODE_PRIVATE);
        boolean nativeMode=prefs.getBoolean("native_mode",true);
        if(!prefs.getBoolean(nativeMode?"native_auto":"auto_bt",false))return;
        if(Build.VERSION.SDK_INT>=31
            &&context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)return;
        try{
            BluetoothDevice device=Build.VERSION.SDK_INT>=33
                ?intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE,BluetoothDevice.class)
                :(BluetoothDevice)intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if(device==null)return;
            BluetoothClass cls=device.getBluetoothClass();
            if(cls!=null&&cls.getMajorDeviceClass()!=BluetoothClass.Device.Major.AUDIO_VIDEO)return;
            String address=device.getAddress();
            String name=device.getName();
            if(nativeMode) {
                String profile=prefs.getString("native_bt_profile_"+address,
                    prefs.getString("native_bt_default","Paredão"));
                NativeMusicFxPresets.select(prefs,profile);
                prefs.edit().putString("native_bt_id",address)
                    .putString("native_bt_name",name==null?"Bluetooth":name)
                    .putString("native_bt_status","Referência "+profile+" selecionada ao conectar. Ajuste o MusicFX manualmente.")
                    .putBoolean("native_bt_active",true).apply();
                return;
            }
            String profile=prefs.getString("bt_profile_"+address,prefs.getString("bt_default","Paredão"));
            AudioProfiles.setPreset(prefs,profile);
            prefs.edit().putString("bt_current",address)
                .putString("bt_name",name==null?"Dispositivo Bluetooth":name)
                .putBoolean("enabled",true)
                .putString("status","Bluetooth conectado. Iniciando efeito experimental...").apply();
            context.startForegroundService(new Intent(context,AudioEffectsService.class)
                .setAction(AudioEffectsService.ACTION_START));
        }catch(RuntimeException e){
            prefs.edit().putString("native_bt_status",
                "Conexão Bluetooth detectada. Abra o app para escolher a referência.").apply();
        }
    }
}
