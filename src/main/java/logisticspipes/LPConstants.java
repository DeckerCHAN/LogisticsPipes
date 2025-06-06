package logisticspipes;

public class LPConstants {

    private LPConstants() {}

    public static final float FACADE_THICKNESS = 2F / 16F;
    public static final float PIPE_NORMAL_SPEED = 0.01F;
    public static final float PIPE_MIN_POS = 0.1875F;
    public static final float PIPE_MAX_POS = 0.8125F;
    public static final float BC_PIPE_MIN_POS = 0.25F;
    public static final float BC_PIPE_MAX_POS = 0.75F;

    public static final boolean DEBUG = Boolean.getBoolean("logisticspipes.enableDebug");

    public static int pipeModel = -1;
    public static int solidBlockModel = -1;

    public static final String computerCraftModID = "ComputerCraft@1.7";
    public static final String openComputersModID = "OpenComputers";

    public static boolean COREMOD_LOADED = false;
}
