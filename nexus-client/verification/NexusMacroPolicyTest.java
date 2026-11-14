package com.port.nexusclient;

import java.util.UUID;

public final class NexusMacroPolicyTest {
    private static void ok(boolean value,String reason){
        if(!value)throw new AssertionError(reason);
    }
    public static void main(String[] args){
        var s=new NexusMacroPolicy.State();
        UUID a=UUID.randomUUID(),b=UUID.randomUUID();
        var one=NexusMacroPolicy.register(s,a,1000,3,5000,17000);
        ok(one.hits()==1&&!one.trigger(),"First hit should advance the combo");
        var duplicate=NexusMacroPolicy.register(s,a,1060,3,5000,17000);
        ok(duplicate.hits()==1&&!duplicate.trigger(),"Double event is not a second hit");
        ok(NexusMacroPolicy.register(s,a,1800,3,5000,17000).hits()==2,
            "Second distinct hit advances counter");
        var triggered=NexusMacroPolicy.register(s,a,2500,3,5000,17000);
        ok(triggered.trigger()&&s.hits==0,"Third hit triggers one cast and clears counter");
        ok(!NexusMacroPolicy.register(s,a,3400,3,5000,17000).trigger(),
            "Macro cannot auto-cast during rearm cooldown");
        s.resetAll();
        NexusMacroPolicy.register(s,a,1000,3,5000,17000);
        var changed=NexusMacroPolicy.register(s,b,1700,3,5000,17000);
        ok(changed.hits()==1,"Changing target restarts combo");
        NexusMacroPolicy.register(s,b,2400,3,5000,17000);
        ok(NexusMacroPolicy.register(s,b,3600,3,5000,17000).trigger(),
            "Third hit on the same new target works");
        s.resetAll();
        NexusMacroPolicy.register(s,a,1000,3,5000,17000);
        NexusMacroPolicy.register(s,a,2200,3,5000,17000);
        var tooLate=NexusMacroPolicy.register(s,a,6200,3,5000,17000);
        ok(tooLate.hits()==1&&!tooLate.trigger(),"Window timeout restarts combo");
        ok(NexusMacroPolicy.windowMs(0)==2000&&NexusMacroPolicy.windowMs(100)==10000,
            "Invalid windows are constrained");
        ok(NexusMacroPolicy.register(s,null,7000,3,5000,17000).reason().equals("invalid"),
            "Null targets cannot trigger a spell");
        ok(NexusMacroPolicy.register(s,a,-5,3,5000,17000).reason().equals("invalid"),
            "Invalid timestamps fail closed");
        s.resetAll();
        ok(NexusMacroPolicy.register(s,a,1000,2,5000,17000).hits()==1,
            "Two-hit macro supported");
        ok(NexusMacroPolicy.register(s,a,2000,2,5000,17000).trigger(),
            "Two-hit configured macro triggers");
        System.out.println("NexusMacroPolicyTest OK");
    }
}
