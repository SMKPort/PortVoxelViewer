import dev.port.armourbend.ArmSide;

public final class TestArmSide {
    public static void main(String[] args) {
        verify("Hand_R", ArmSide.RIGHT);
        verify("Forearm_R", ArmSide.RIGHT);
        verify("Arm_R2", ArmSide.RIGHT);
        verify("Antebraco_Direito", ArmSide.RIGHT);
        verify("Cotovelo_L", ArmSide.LEFT);
        verify("LowerArm_L", ArmSide.LEFT);
        verify("Forearm_Left", ArmSide.LEFT);
        verify("Bone2", null);
        verify("Arm_R", null);
        verify("Nexus", null);
        System.out.println("10/10 arm-name checks passed.");
    }
    private static void verify(String text, ArmSide expected) {
        ArmSide actual = ArmSide.fromChildName(text);
        if (actual != expected) {
            throw new AssertionError(text + ": expected=" + expected + ", actual=" + actual);
        }
    }
}
