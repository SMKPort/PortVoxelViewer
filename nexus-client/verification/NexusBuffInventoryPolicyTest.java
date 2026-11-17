package com.port.nexusclient;

public final class NexusBuffInventoryPolicyTest {
    private static void check(boolean ok,String context){
        if(!ok)throw new AssertionError(context);
    }
    public static void main(String[] args){
        check(NexusBuffInventoryPolicy.mainInventoryIndex(9),"First main row");
        check(NexusBuffInventoryPolicy.mainInventoryIndex(35),"Last main row");
        check(!NexusBuffInventoryPolicy.mainInventoryIndex(36),"No hotbar/menu armor slots");
        check(!NexusBuffInventoryPolicy.mainInventoryIndex(8),"Hotbar not source");
        boolean[] full={false,false,false,false,false,false,false,false,false};
        check(NexusBuffInventoryPolicy.reserveHotbar(full,8)==7,
            "Avoid the selected hand when all slots occupied");
        boolean[] empty={false,false,false,false,false,false,false,false,true};
        check(NexusBuffInventoryPolicy.reserveHotbar(empty,1)==8,
            "Prefer an empty temporary hotbar slot");
        check(NexusBuffInventoryPolicy.reserveHotbar(empty,8)==7,
            "Do not displace the selected slot even when empty");
        check(NexusBuffInventoryPolicy.reserveHotbar(new boolean[7],1)<0,
            "Reject malformed inventories");
        check(NexusBuffInventoryPolicy.safeRestore(true,true,true,true),
            "Restore only if original hotbar stack still occupies source.");
        check(!NexusBuffInventoryPolicy.safeRestore(false,true,true,true),
            "Never overwrite a source stack changed by the user or server");
        check(!NexusBuffInventoryPolicy.safeRestore(true,false,true,true),
            "Never overwrite a temporary slot changed by another action");
        check(!NexusBuffInventoryPolicy.safeRestore(true,true,false,true),
            "Never restore when the player changed selected slot");
        check(!NexusBuffInventoryPolicy.safeRestore(true,true,true,false),
            "Use only the vanilla inventory menu");
        System.out.println("NexusBuffInventoryPolicyTest OK");
    }
}
