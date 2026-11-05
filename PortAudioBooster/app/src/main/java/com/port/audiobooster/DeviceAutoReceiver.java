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

/** Conexão Bluetooth: carrega preset próprio, sem integração com equalizador externo. */
public final class DeviceAutoReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        if(intent==null||!BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction()))return;
        SharedPreferences p=context.getSharedPreferences(GlobalAudioEngine.PREFS,Context.MODE_PRIVATE);
        if(!p.getBoolean("auto_bt",false))return;
        if(Build.VERSION.SDK_INT>=31&&context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
            !=PackageManager.PERMISSION_GRANTED)return;
        try{
            BluetoothDevice d=Build.VERSION.SDK_INT>=33
                ?intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE,BluetoothDevice.class)
                :(BluetoothDevice)intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if(d==null)return;
            BluetoothClass cls=d.getBluetoothClass();
            if(cls!=null&&cls.getMajorDeviceClass()!=BluetoothClass.Device.Major.AUDIO_VIDEO)return;
            String address=d.getAddress();
            String name=d.getName();
            String chosen=p.getString("bt_profile_"+address,p.getString("bt_car_default","Paredão Controlado"));
            CarAudioPresets.apply(p,chosen);
            p.edit().putString("bt_current",address).putString("bt_name",name==null?"Bluetooth":name)
                .putBoolean("car_guard",true).putBoolean("enabled",true).putBoolean("compare_original",false)
                .putString("status","Conexão Bluetooth. Tentando iniciar processamento...").apply();
            try{
                context.startForegroundService(new Intent(context,AudioEffectsService.class)
                    .setAction(AudioEffectsService.ACTION_START));
            }catch(RuntimeException blocked){
                p.edit().putString("status","Android restringiu início em segundo plano. Abra o Port Audio Booster.").apply();
            }
        }catch(RuntimeException ignored){
            p.edit().putString("status","Falha na detecção Bluetooth. Abra o app manualmente.").apply();
        }
    }
}
