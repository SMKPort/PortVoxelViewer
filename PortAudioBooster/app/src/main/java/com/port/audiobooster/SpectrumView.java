package com.port.audiobooster;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.audiofx.Visualizer;
import android.view.View;

/** Visualizador real via Visualizer(0) quando disponível e autorizado. Sem dados sintéticos. */
final class SpectrumView extends View {
    private final Paint paint=new Paint(3);
    private final float[] levels=new float[28];
    private Visualizer visualizer;
    private volatile boolean samples;
    private String status="Toque para iniciar a captura";
    SpectrumView(Context c){super(c);setMinimumHeight((int)(210*getResources().getDisplayMetrics().density));}
    void start(){
        stop();
        try{
            visualizer=new Visualizer(0);
            int[] range=Visualizer.getCaptureSizeRange();
            visualizer.setCaptureSize(Math.min(range[1],512));
            int rc=visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener(){
                @Override public void onWaveFormDataCapture(Visualizer v,byte[] data,int rate){}
                @Override public void onFftDataCapture(Visualizer v,byte[] fft,int rate){
                    if(fft==null||fft.length<4)return;
                    int half=fft.length/2;
                    for(int i=0;i<levels.length;i++){
                        double a=Math.exp(Math.log(1)+(Math.log(half)-Math.log(1))*i/levels.length);
                        double b=Math.exp(Math.log(1)+(Math.log(half)-Math.log(1))*(i+1d)/levels.length);
                        int lo=Math.max(1,Math.min(half-1,(int)a));
                        int hi=Math.max(lo+1,Math.min(half,(int)b));
                        double max=0;
                        for(int k=lo;k<hi;k++){
                            double re=fft[2*k],im=fft[2*k+1];
                            max=Math.max(max,Math.sqrt(re*re+im*im));
                        }
                        levels[i]=(float)Math.min(1d,Math.log1p(max)/6d);
                        if(max>0.5) samples=true;
                    }
                    postInvalidate();
                }
            }, Math.min(Visualizer.getMaxCaptureRate(),15000),false,true);
            if(rc!=Visualizer.SUCCESS)throw new IllegalStateException("Erro "+rc);
            visualizer.setEnabled(true);
            status="Aguardando amostras do áudio do sistema";
        }catch(RuntimeException|LinkageError e){
            stop();status="Indisponível neste Android: "+e.getClass().getSimpleName();
        }
        invalidate();
    }
    void stop(){
        if(visualizer!=null){
            try{visualizer.setEnabled(false);}catch(RuntimeException ignored){}
            try{visualizer.release();}catch(RuntimeException ignored){}
            visualizer=null;
        }
        samples=false;
        for(int i=0;i<levels.length;i++)levels[i]=0;
    }
    String getStatus(){return samples?"Espectro capturado (origem de áudio não verificada)":status;}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        int w=getWidth(),h=getHeight();
        c.drawColor(Color.rgb(29,23,30));
        float left=12f*getResources().getDisplayMetrics().density;
        float width=(w-2*left)/levels.length;
        for(int i=0;i<levels.length;i++){
            paint.setColor(i<levels.length/3?Color.rgb(204,38,57):Color.rgb(231,183,75));
            float height=Math.max(2,levels[i]*(h-20));
            c.drawRoundRect(left+i*width,h-height-7,left+(i+0.65f)*width,h-7,3,3,paint);
        }
        if(!samples){
            paint.setColor(Color.rgb(210,190,180));paint.setTextSize(14*getResources().getDisplayMetrics().scaledDensity);
            c.drawText("Sem sinal confirmado",left,28*getResources().getDisplayMetrics().density,paint);
        }
    }
}
