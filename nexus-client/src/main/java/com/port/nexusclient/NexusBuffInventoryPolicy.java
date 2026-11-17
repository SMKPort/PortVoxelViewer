package com.port.nexusclient;

/**
 * Vanilla player inventory: hotbar 0..8, main rows 9..35.
 * InventoryMenu has the same 9..35 IDs for main rows; SWAP button=0..8.
 */
final class NexusBuffInventoryPolicy {
    private NexusBuffInventoryPolicy(){}
    static boolean mainInventoryIndex(int index){return index>=9&&index<=35;}
    static int reserveHotbar(boolean[] emptySlots,int selected){
        if(emptySlots==null||emptySlots.length!=9||selected<0||selected>8)return -1;
        for(int i=8;i>=0;i--)
            if(i!=selected&&emptySlots[i])return i;
        for(int i=8;i>=0;i--)
            if(i!=selected)return i;
        return -1;
    }
    static boolean safeRestore(boolean oldHotbarRemainsAtSource,
                               boolean buffOrEmptyInTempSlot,boolean userStillInOriginalSlot,
                               boolean playerInventoryMenuOpen){
        return oldHotbarRemainsAtSource&&buffOrEmptyInTempSlot&&
            userStillInOriginalSlot&&playerInventoryMenuOpen;
    }
}
