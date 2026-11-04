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

/** Automação opt-in; broadcasts e serviços em background podem ser limitados pelo Android. */
public final class DeviceAutoReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        if(intent==null||!BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction()))return;
        SharedPreferences prefs=context.getSharedPreferences(GlobalAudioEngine.PREFS,Context.MODE_PRIVATE);
        if(!prefs.getBoolean("auto_bt",false))return;
        if(Build.VERSION.SDK_INT>=31&&context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)return;
        try{
            BluetoothDevice device=Build.VERSION.SDK_INT>=33
                ?intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE,BluetoothDevice.class)
                :(BluetoothDevice)intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if(device==null)return;
            BluetoothClass cls=device.getBluetoothClass();
            if(cls!=null&&cls.getMajorDeviceClass()!=BluetoothClass.Device.Major.AUDIO_VIDEO)return;
            String address=device.getAddress();
            String name=device.getName();
            String chosen=prefs.getString("bt_profile_"+address,prefs.getString("bt_default","Paredão"));
            AudioProfiles.setPreset(prefs,chosen);
            prefs.edit().putString("bt_current",address).putString("bt_name",name==null?"Dispositivo Bluetooth":name)
                .putBoolean("enabled",true).putString("status","Bluetooth conectado. Iniciando efeito experimental...").apply();
            context.startForegroundService(new Intent(context,AudioEffectsService.class)
                .setAction(AudioEffectsService.ACTION_START));
        }catch(RuntimeException e){
            prefs.edit().putString("status","Bluetooth conectado. Abra o app para ligar o efeito (restrição do Android).").apply();
        }
    }
}
