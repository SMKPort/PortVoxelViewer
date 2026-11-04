package com.port.audiobooster;

import android.content.SharedPreferences;
import android.media.audiofx.BassBoost;
import android.media.audiofx.DynamicsProcessing;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.Virtualizer;
import android.os.Build;
import java.util.Locale;

/**
 * Efeitos na sessão de saída 0: recurso LEGADO/EXPERIMENTAL.
 * O Android pode ignorar o processamento do Spotify apesar de API disponível.
 */
final class GlobalAudioEngine {
    static final String PREFS="audio_global_settings";
    private Equalizer equalizer;
    private BassBoost bass;
    private Virtualizer virtualizer;
    private LoudnessEnhancer loudness;
    private DynamicsProcessing dynamics;
    private String status="Inativo";
    String getStatus(){return status;}

    void start(){
        close();
        try{equalizer=new Equalizer(100,0);}catch(RuntimeException|LinkageError e){releaseEq();}
        try{bass=new BassBoost(100,0);}catch(RuntimeException|LinkageError e){releaseBass();}
        try{virtualizer=new Virtualizer(100,0);}catch(RuntimeException|LinkageError e){releaseSpace();}
        if(Build.VERSION.SDK_INT>=28)try{dynamics=new DynamicsProcessing(0);}
            catch(RuntimeException|LinkageError e){releaseDynamics();}
        try{loudness=new LoudnessEnhancer(0);}catch(RuntimeException|LinkageError e){releaseLoudness();}
        updateStatus(false);
    }

    void apply(SharedPreferences prefs){
        boolean original=prefs.getBoolean("compare_original",false);
        if(equalizer!=null){
            try{
                int[] bounds={equalizer.getBandLevelRange()[0],equalizer.getBandLevelRange()[1]};
                for(short i=0;i<equalizer.getNumberOfBands();i++){
                    double hz=equalizer.getCenterFreq(i)/1000d;
                    double db=AudioProfiles.atHz(prefs,hz)+AudioProfiles.bassDb(prefs,hz);
                    int mb=(int)Math.round(AudioProfiles.clamp((int)Math.round(db*100),bounds[0],bounds[1]));
                    equalizer.setBandLevel(i,(short)(original?0:mb));
                }
                equalizer.setEnabled(!original);
            }catch(RuntimeException|LinkageError ignored){}
        }
        if(bass!=null)try{
            int p=AudioProfiles.clamp(prefs.getInt("bass",35),0,100);
            bass.setStrength((short)(p*10));
            bass.setEnabled(!original&&p>0);
        }catch(RuntimeException|LinkageError ignored){}
        if(virtualizer!=null)try{
            int p=AudioProfiles.clamp(prefs.getInt("space",0),0,100);
            virtualizer.setStrength((short)(p*10));
            virtualizer.setEnabled(!original&&p>0);
        }catch(RuntimeException|LinkageError ignored){}

        int gain=AudioProfiles.clamp(prefs.getInt("gain",0),0,6);
        boolean protection=prefs.getBoolean("safe_gain",true);
        boolean dynamicActive=false;
        if(dynamics!=null)try{
            // Modo inteligente: ganho de entrada e limiter digital em conjunto,
            // caso o fornecedor implemente DynamicsProcessing na sessão 0.
            dynamics.setInputGainAllChannelsTo(original?0f:(float)gain);
            DynamicsProcessing.Limiter limiter=new DynamicsProcessing.Limiter(
                true,!original&&protection,0,1f,80f,15f,-1.0f,0f);
            dynamics.setLimiterAllChannelsTo(limiter);
            dynamics.setEnabled(!original);
            dynamicActive=dynamics.hasControl();
        }catch(RuntimeException|LinkageError ignored){}
        if(loudness!=null)try{
            // Sem compressor do Android, limitar ganho conservadoramente.
            int effective=dynamicActive?0:(protection?Math.min(gain,2):gain);
            loudness.setTargetGain((original?0:effective)*100);
            loudness.setEnabled(!original&&effective>0);
        }catch(RuntimeException|LinkageError ignored){}
        updateStatus(original);
    }

    private void updateStatus(boolean original){
        if(original){status="A/B: ORIGINAL (efeitos desativados).";return;}
        boolean any=(equalizer!=null&&equalizer.hasControl())||(bass!=null&&bass.hasControl())
             ||(virtualizer!=null&&virtualizer.hasControl())||(loudness!=null&&loudness.hasControl())
             ||(dynamics!=null&&dynamics.hasControl());
        if(!any){status="Nenhum efeito global controlável. Use o equalizador do Spotify/Android.";return;}
        status=String.format(Locale.US,
            "API global: EQ %s | Bass %s | Espaço %s | Ganho %s | Limiter %s. Verificação auditiva obrigatória.",
            marker(equalizer!=null&&equalizer.hasControl()),
            marker(bass!=null&&bass.hasControl()),
            marker(virtualizer!=null&&virtualizer.hasControl()),
            marker(loudness!=null&&loudness.hasControl()),
            marker(dynamics!=null&&dynamics.hasControl()));
    }
    private String marker(boolean value){return value?"OK":"N/D";}
    private void releaseEq(){if(equalizer!=null){try{equalizer.release();}catch(RuntimeException ignored){}equalizer=null;}}
    private void releaseBass(){if(bass!=null){try{bass.release();}catch(RuntimeException ignored){}bass=null;}}
    private void releaseSpace(){if(virtualizer!=null){try{virtualizer.release();}catch(RuntimeException ignored){}virtualizer=null;}}
    private void releaseLoudness(){if(loudness!=null){try{loudness.release();}catch(RuntimeException ignored){}loudness=null;}}
    private void releaseDynamics(){if(dynamics!=null){try{dynamics.release();}catch(RuntimeException ignored){}dynamics=null;}}
    void close(){releaseEq();releaseBass();releaseSpace();releaseLoudness();releaseDynamics();status="Inativo";}
}
