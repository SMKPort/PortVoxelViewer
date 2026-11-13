package com.port.nexusclient;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Streaming voice activity detection, segmentation and mono 16 kHz PCM conversion.
 * No Minecraft or Plasmo dependencies. Keeps only the current utterance in RAM.
 */
final class NexusVoiceSegmenter {
    static final int RATE=16000;
    static final int MAX_SECONDS=12;
    static final int MAX_BYTES=RATE*MAX_SECONDS*2;
    private static final int FRAME_SAMPLES=320; // ~20 ms
    private static final int PRE_ROLL_BYTES=RATE*2*180/1000;
    private static final int OVERLAP_BYTES=RATE*2*200/1000;
    private static final int MIN_VOICE_SAMPLES=RATE*280/1000;
    // Wait slightly longer across natural pauses, combining short words into
    // one request without retaining voice history.
    private static final int SILENCE_SAMPLES=RATE*800/1000;
    private static final long IDLE_MS=680;

    static final class Segment {
        final byte[] pcm;
        final long startedAtMs;
        final long finishedAtMs;
        Segment(byte[] pcm,long startedAtMs,long finishedAtMs){
            this.pcm=pcm;this.startedAtMs=startedAtMs;this.finishedAtMs=finishedAtMs;
        }
        int samples(){return pcm.length/2;}
    }

    private final int sensitivity;
    private final ByteArrayOutputStream preRoll=new ByteArrayOutputStream(PRE_ROLL_BYTES);
    private final ByteArrayOutputStream active=new ByteArrayOutputStream(MAX_BYTES);
    private long startedAtMs,lastFrameMs=-1;
    private boolean speaking;
    private int voicedSamples,silenceSamples;
    private double noiseFloor=60;

    NexusVoiceSegmenter(int sensitivity){
        this.sensitivity=Math.max(20,Math.min(100,sensitivity));
    }

    List<Segment> accept(short[] source,int rate,int channels,long now){
        List<Segment> finished=new ArrayList<>();
        if(source==null||source.length==0||rate<8000||rate>48000||
           channels<1||channels>2)return finished;
        short[] mono=resample(source,rate,channels);
        for(int offset=0;offset<mono.length;offset+=FRAME_SAMPLES){
            int count=Math.min(FRAME_SAMPLES,mono.length-offset);
            byte[] frame=new byte[count*2];
            double power=0;
            for(int i=0;i<count;i++){
                int sample=mono[offset+i];
                power+=(double)sample*sample;
                frame[i*2]=(byte)(sample&255);
                frame[i*2+1]=(byte)((sample>>>8)&255);
            }
            double rms=Math.sqrt(power/Math.max(1,count));
            double threshold=Math.max(120,920-sensitivity*11);
            boolean loud=rms>Math.max(threshold,noiseFloor*2.6);
            if(!speaking){
                if(loud){
                    speaking=true;
                    startedAtMs=now-(preRoll.size()*1000L)/(RATE*2);
                    active.writeBytes(preRoll.toByteArray());
                    voicedSamples=0;silenceSamples=0;
                }else{
                    noiseFloor=.95*noiseFloor+.05*Math.min(900,rms);
                }
            }
            if(speaking){
                active.writeBytes(frame);
                if(loud){voicedSamples+=count;silenceSamples=0;}
                else silenceSamples+=count;
                if(active.size()>=MAX_BYTES){
                    Segment segment=finish(now);
                    if(segment!=null)finished.add(segment);
                    // Keep ~200ms from the previous chunk so split speech keeps phonemes.
                    byte[] tail=segment==null?new byte[0]:
                        Arrays.copyOfRange(segment.pcm,
                            Math.max(0,segment.pcm.length-OVERLAP_BYTES),segment.pcm.length);
                    speaking=true;startedAtMs=now-200;active.writeBytes(tail);
                    voicedSamples=0;silenceSamples=0;
                }else if(silenceSamples>=SILENCE_SAMPLES){
                    Segment segment=finish(now);
                    if(segment!=null)finished.add(segment);
                }
            }
            putPreRoll(frame);
            lastFrameMs=now;
        }
        return finished;
    }

    Segment flushIdle(long now){
        if(!speaking||lastFrameMs<0||now-lastFrameMs<IDLE_MS)return null;
        return finish(lastFrameMs);
    }

    void reset(){
        preRoll.reset();active.reset();speaking=false;voicedSamples=0;
        silenceSamples=0;lastFrameMs=-1;noiseFloor=60;
    }

    private Segment finish(long now){
        byte[] clip=active.toByteArray();
        Segment completed=voicedSamples>=MIN_VOICE_SAMPLES&&clip.length>=2?
            new Segment(clip,startedAtMs,now):null;
        active.reset();speaking=false;voicedSamples=0;silenceSamples=0;
        return completed;
    }

    private void putPreRoll(byte[] data){
        byte[] old=preRoll.toByteArray();
        int take=Math.min(old.length,Math.max(0,PRE_ROLL_BYTES-data.length));
        preRoll.reset();
        if(take>0)preRoll.write(old,old.length-take,take);
        if(data.length>PRE_ROLL_BYTES)
            preRoll.write(data,data.length-PRE_ROLL_BYTES,PRE_ROLL_BYTES);
        else preRoll.writeBytes(data);
    }

    private static short[] resample(short[] input,int rate,int channels){
        int frames=input.length/channels;
        int needed=(int)Math.round(frames*(double)RATE/rate);
        short[] output=new short[Math.max(0,needed)];
        for(int out=0;out<output.length;out++){
            int from=Math.min(frames-1,(int)((long)out*rate/RATE));
            int to=Math.min(frames,Math.max(from+1,(int)((long)(out+1)*rate/RATE)));
            long sum=0;int n=0;
            for(int j=from;j<to;j++)
                for(int channel=0;channel<channels;channel++){
                    sum+=input[j*channels+channel];n++;
                }
            output[out]=(short)(sum/Math.max(1,n));
        }
        return output;
    }
}
