package com.port.nexusclient;

/** No Minecraft dependencies: verifies scope selection and provider limits. */
public final class NexusVisionModesTest {
    private static void check(boolean ok,String detail){
        if(!ok)throw new AssertionError(detail);
    }
    public static void main(String[] args){
        var modes=NexusVisionModes.Focus.values();
        check(modes.length==4,"The manual UI must offer exactly four supported modes.");
        var cycle=NexusVisionModes.Focus.GERAL;
        for(int i=0;i<modes.length;i++)cycle=NexusVisionModes.next(cycle);
        check(cycle==NexusVisionModes.Focus.GERAL,"All modes must cycle predictably.");
        for(var mode:modes){
            String result=NexusVisionModes.prompt(mode,"Detalhe um elemento visível.");
            check(result.length()<=180&&result.contains("Pergunta: "),
                "Prompt respects the HTTPS relay's 180-character limit");
            check(result.contains("Detalhe um elemento"),"The original user question is included.");
            String huge=NexusVisionModes.prompt(mode,"x".repeat(1000));
            check(huge.length()<=180,"Huge user input is safely clamped.");
            check(!huge.contains("\n"),"No raw control characters.");
        }
        check(NexusVisionModes.prompt(NexusVisionModes.Focus.GERAL,"\nBom  \t dia\n")
            .contains("Bom dia"),"Whitespace normalized.");
        check(NexusVisionModes.prompt(NexusVisionModes.Focus.PERIGOS,null)
            .contains("Pergunta:"),"Missing user question has a useful default.");
        check(NexusVisionModes.prompt(NexusVisionModes.Focus.PERIGOS,"Tem mobs invisíveis?")
            .contains("não invente mobs ocultos"),"Warnings must avoid fabricated hidden threats.");
        System.out.println("NexusVisionModesTest OK");
    }
}
