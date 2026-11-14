package com.port.nexusclient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** Regression: the official Vosk Portuguese model 0.3 is a FLAT layout. */
public final class NexusVoiceModelLayoutTest {
    private static void assertThat(boolean condition,String note){
        if(!condition)throw new AssertionError(note);
    }
    private static void create(Path dir,String... paths)throws Exception{
        for(String entry:paths){
            Path path=dir.resolve(entry);
            Files.createDirectories(path.getParent());
            Files.writeString(path,"stub");
        }
    }
    private static void remove(Path dir)throws Exception{
        try(var contents=Files.walk(dir)){
            for(Path p:contents.sorted(Comparator.reverseOrder()).toList())
                Files.deleteIfExists(p);
        }
    }
    public static void main(String[] args)throws Exception{
        Path dir=Files.createTempDirectory("nexus-vosk-layout-");
        try{
            Path pt=dir.resolve("vosk-model-small-pt-0.3");
            create(pt,"final.mdl","mfcc.conf","HCLr.fst","Gr.fst",
                "word_boundary.int","ivector/final.ie");
            assertThat(NexusVoiceModelLayout.inspect(pt)==NexusVoiceModelLayout.Format.LEGACY,
                "Real official PT model 0.3 legacy layout must pass");
            assertThat(NexusVoiceModelLayout.valid(pt),"Legacy format is loadable by native Vosk");
            Path en=dir.resolve("vosk-model-small-en-us-0.15");
            create(en,"am/final.mdl","conf/model.conf","conf/mfcc.conf",
                "graph/HCLr.fst","graph/Gr.fst");
            assertThat(NexusVoiceModelLayout.inspect(en)==NexusVoiceModelLayout.Format.MODERN,
                "Modern English model must still pass");
            Path staticGraph=dir.resolve("vosk-other-static");
            create(staticGraph,"am/final.mdl","conf/model.conf","conf/mfcc.conf",
                "graph/HCLG.fst");
            assertThat(NexusVoiceModelLayout.valid(staticGraph),
                "Modern model with static graph should still be valid");
            Path unfinished=dir.resolve("unfinished");
            create(unfinished,"final.mdl","mfcc.conf","HCLr.fst");
            assertThat(!NexusVoiceModelLayout.valid(unfinished),
                "Truly missing graphs must be rejected");
            Path empty=dir.resolve("empty");
            create(empty,"final.mdl","mfcc.conf","HCLr.fst","Gr.fst");
            Files.write(empty.resolve("final.mdl"),new byte[0]);
            assertThat(!NexusVoiceModelLayout.valid(empty),
                "Empty critical files must not count as valid");
            assertThat(!NexusVoiceModelLayout.valid(dir.resolve("missing")),
                "Missing model directory must be rejected");
            System.out.println("NexusVoiceModelLayoutTest PASS (legacy PT, modern EN, static graph, incomplete)");
        }finally{remove(dir);}
    }
}
