package com.port.nexusclient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Vosk ships both the modern am/conf/graph tree and an older flat layout.
 * The official Brazilian Portuguese small model uses the flat layout.
 * Vosk itself supports both (Model::ConfigureV1 and ConfigureV2).
 */
final class NexusVoiceModelLayout {
    enum Format { MODERN, LEGACY, INVALID }

    private NexusVoiceModelLayout(){}

    static Format inspect(Path folder){
        if(folder==null||!Files.isDirectory(folder))return Format.INVALID;
        if(hasFiles(folder,List.of("am/final.mdl","conf/model.conf","conf/mfcc.conf"))
            &&hasGraph(folder,"graph/"))
            return Format.MODERN;
        if(hasFiles(folder,List.of("final.mdl","mfcc.conf"))
            &&hasGraph(folder,""))
            return Format.LEGACY;
        return Format.INVALID;
    }

    static boolean valid(Path folder){
        return inspect(folder)!=Format.INVALID;
    }

    static String describe(Path folder){
        return switch(inspect(folder)){
            case MODERN -> "estrutura moderna (am/conf/graph)";
            case LEGACY -> "estrutura legada portuguesa (arquivos na raiz)";
            case INVALID -> "arquivos essenciais ausentes";
        };
    }

    private static boolean hasGraph(Path folder,String prefix){
        return file(folder,prefix+"HCLG.fst")||
            (file(folder,prefix+"HCLr.fst")&&file(folder,prefix+"Gr.fst"));
    }
    private static boolean hasFiles(Path folder,List<String> paths){
        for(String part:paths)if(!file(folder,part))return false;
        return true;
    }
    private static boolean file(Path folder,String path){
        try{
            Path target=folder.resolve(path);
            return Files.isRegularFile(target)&&Files.size(target)>0;
        }catch(Exception ignored){return false;}
    }
}
