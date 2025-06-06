package logisticspipes.proxy.cc.wrapper;

import net.minecraftforge.common.util.ForgeDirection;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import logisticspipes.blocks.LogisticsSolidTileEntity;
import logisticspipes.proxy.computers.wrapper.CCObjectWrapper;

public class LPPeripheralTileSolidWrapper implements IPeripheral {

    private final CCCommandWrapper wrapped;
    private final LogisticsSolidTileEntity tile;

    public LPPeripheralTileSolidWrapper(LogisticsSolidTileEntity tile, ForgeDirection dir) {
        this.tile = tile;
        wrapped = (CCCommandWrapper) CCObjectWrapper.checkForAnnotations(tile, CCCommandWrapper.WRAPPER);
    }

    @Override
    public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) {
        wrapped.isDirectCall = true;
        Object[] result = wrapped.callMethod(context, method, arguments);
        wrapped.isDirectCall = false;
        return result;
    }

    @Override
    public void attach(IComputerAccess computer) {}

    @Override
    public void detach(IComputerAccess computer) {}

    @Override
    public boolean equals(IPeripheral other) {
        if (other instanceof LPPeripheralTileSolidWrapper) {
            return ((LPPeripheralTileSolidWrapper) other).tile.equals(tile);
        }
        return false;
    }

    @Override
    public String getType() {
        return wrapped.getType();
    }

    @Override
    public String[] getMethodNames() {
        return wrapped.getMethodNames();
    }
}
