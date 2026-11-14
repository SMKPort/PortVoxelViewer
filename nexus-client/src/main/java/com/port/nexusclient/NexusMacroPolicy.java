package com.port.nexusclient;

import java.util.UUID;

/** Pure combo state machine. No packets, chat, APIs or Minecraft dependencies. */
final class NexusMacroPolicy {
    static final long MIN_HIT_INTERVAL_MS=275L;
    static final long DEFAULT_WINDOW_MS=5_000L;
    static final long DEFAULT_REARM_MS=17_000L;
    static final int DEFAULT_HITS=3;
    static final int MIN_HITS=2,MAX_HITS=6;

    static final class State {
        UUID target;
        int hits;
        long firstHitAt=-1,lastHitAt=-1,lastCastAt=-1;
        void resetCombo(){target=null;hits=0;firstHitAt=-1;lastHitAt=-1;}
        void resetAll(){resetCombo();lastCastAt=-1;}
    }
    record Result(int hits,boolean trigger,String reason){}

    private NexusMacroPolicy(){}

    static Result register(State s,UUID target,long now,int requiredHits,
                           long windowMs,long rearmMs){
        if(s==null||target==null||now<0)return new Result(0,false,"invalid");
        int required=Math.max(MIN_HITS,Math.min(MAX_HITS,requiredHits));
        long window=Math.max(1_500,Math.min(10_000,windowMs));
        long cooldown=Math.max(1_000,rearmMs);
        if(s.lastCastAt>=0&&now-s.lastCastAt<cooldown){
            s.resetCombo();
            return new Result(0,false,"cooldown");
        }
        if(s.target==null||!s.target.equals(target)||
           s.firstHitAt<0||now<s.lastHitAt||now-s.firstHitAt>window){
            s.target=target;s.hits=0;s.firstHitAt=now;s.lastHitAt=-1;
        }
        if(s.lastHitAt>=0&&now-s.lastHitAt<MIN_HIT_INTERVAL_MS)
            return new Result(s.hits,false,"duplicate");
        s.lastHitAt=now;
        s.hits++;
        if(s.hits>=required){
            s.resetCombo();
            s.lastCastAt=now;
            return new Result(required,true,"ready");
        }
        return new Result(s.hits,false,"progress");
    }

    static long windowMs(int seconds){
        return Math.max(2,Math.min(10,seconds))*1_000L;
    }
}
