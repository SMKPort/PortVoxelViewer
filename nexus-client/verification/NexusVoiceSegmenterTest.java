package com.port.nexusclient;

import java.util.List;
import java.util.ArrayList;

/** Pure Java tests; no Minecraft, NeoForge, Plasmo, or network. */
public final class NexusVoiceSegmenterTest {
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    private static short[] chunk(int sampleRate,int millis,int amplitude){
        short[] data=new short[sampleRate*millis/1000];
        for(int i=0;i<data.length;i++)
            data[i]=(short)(amplitude==0?0:Math.round(Math.sin(i*2*Math.PI*220/sampleRate)*amplitude));
        return data;
    }
    private static void silenceStopsPhrase(){
        var seg=new NexusVoiceSegmenter(55);
        var output=new ArrayList<NexusVoiceSegmenter.Segment>();
        for(int i=0;i<50;i++)output.addAll(seg.accept(chunk(16000,20,2500),16000,1,i*20L));
        for(int i=50;i<95;i++)output.addAll(seg.accept(chunk(16000,20,0),16000,1,i*20L));
        check(output.size()==1,"A pause must finish exactly one utterance");
        check(output.get(0).samples()>12000,"Most of speech should remain intact");
        check(seg.flushIdle(2500)==null,"No double flush");
    }
    private static void longSpeechSplitsWithoutBlocking(){
        var seg=new NexusVoiceSegmenter(60);
        var output=new ArrayList<NexusVoiceSegmenter.Segment>();
        for(int i=0;i<1250;i++)output.addAll(seg.accept(chunk(16000,20,2300),16000,1,i*20L));
        var finalSegment=seg.flushIdle(26000);
        if(finalSegment!=null)output.add(finalSegment);
        check(output.size()>=2,"25s continuous speech must split into multiple requests");
        long total=0;
        for(var s:output){
            check(s.pcm.length<=NexusVoiceSegmenter.MAX_BYTES+640,
                "A segment must fit the API limit");
            total+=s.samples();
        }
        check(total>=16000*24,"Continuous speech should not lose whole intervals");
    }
    private static void noiseAndStereo(){
        var seg=new NexusVoiceSegmenter(50);
        var output=new ArrayList<NexusVoiceSegmenter.Segment>();
        for(int i=0;i<60;i++)
            output.addAll(seg.accept(chunk(16000,20,70),16000,1,i*20L));
        check(output.isEmpty(),"Ambient low amplitude noise is not a voice clip");
        short[] stereo=new short[1920];
        for(int i=0;i<960;i++)stereo[2*i]=stereo[2*i+1]=(short)2000;
        for(int i=0;i<60;i++)output.addAll(seg.accept(stereo,48000,2,1500+i*20));
        check(output.isEmpty(),"Speech before pause must remain buffered");
        var clip=seg.flushIdle(3500);
        check(clip!=null&&clip.samples()>12000,"Stereo 48k must be downmixed to 16k");
        seg.reset();
        check(seg.flushIdle(8000)==null,"Reset must forget uncommitted audio");
    }
    public static void main(String[] args){
        silenceStopsPhrase();longSpeechSplitsWithoutBlocking();noiseAndStereo();
        System.out.println("Nexus Voice Segmenter: 3 test groups passed");
    }
}
