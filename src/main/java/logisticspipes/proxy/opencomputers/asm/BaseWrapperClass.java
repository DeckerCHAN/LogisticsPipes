package logisticspipes.proxy.opencomputers.asm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.prefab.AbstractValue;
import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.blocks.LogisticsSolidTileEntity;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.computers.interfaces.CCCommand;
import logisticspipes.proxy.computers.interfaces.CCDirectCall;
import logisticspipes.proxy.computers.interfaces.CCQueued;
import logisticspipes.proxy.computers.interfaces.ICCTypeWrapped;
import logisticspipes.proxy.computers.objects.CCItemIdentifier.CCItemIdentifierImplementation;
import logisticspipes.proxy.computers.objects.CCItemIdentifierBuilder;
import logisticspipes.proxy.computers.objects.CCItemIdentifierStack.CCItemIdentifierStackImplementation;
import logisticspipes.proxy.computers.objects.LPGlobalCCAccess;
import logisticspipes.proxy.computers.wrapper.CCObjectWrapper;
import logisticspipes.proxy.computers.wrapper.CCWrapperInformation;
import logisticspipes.proxy.computers.wrapper.ICommandWrapper;
import logisticspipes.security.PermissionException;
import logisticspipes.ticks.QueuedTasks;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.LPPosition;

public abstract class BaseWrapperClass extends AbstractValue {

    public static final ICommandWrapper WRAPPER = new ICommandWrapper() {

        private final Map<Class<?>, Class<? extends BaseWrapperClass>> map = new HashMap<>();

        @Override
        public Object getWrappedObject(CCWrapperInformation info, Object object) {
            try {
                Class<? extends BaseWrapperClass> clazz = map.get(object.getClass());
                if (clazz == null) {
                    clazz = ClassCreator.getWrapperClass(info, object.getClass().getName());
                    map.put(object.getClass(), clazz);
                }
                return clazz.getConstructor(CCWrapperInformation.class, Object.class).newInstance(info, object);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    };

    private final CCWrapperInformation info;
    private Object object;
    public boolean isDirectCall;

    public BaseWrapperClass(String wrappedClass) throws ClassNotFoundException {
        this(CCObjectWrapper.getWrapperInformation(Class.forName(wrappedClass)), null);
    }

    public BaseWrapperClass(CCWrapperInformation info, Object object) {
        super();
        this.info = info;
        this.object = object;
    }

    @Callback(direct = true)
    public Object[] help(Context context, Arguments args) throws Exception {
        if (object == null) {
            throw new Exception("This LP object is not persistable");
        }
        StringBuilder help = new StringBuilder();
        StringBuilder head = new StringBuilder();
        StringBuilder head2 = new StringBuilder();
        head.append("Type: ");
        head.append(info.type);
        head.append("\n");
        head2.append("Commands: \n");
        for (Integer num : info.commands.keySet()) {
            Method method = info.commands.get(num);
            StringBuilder command = new StringBuilder();
            if (help.length() != 0) {
                command.append("\n");
            }
            int number = num;
            if (number < 10) {
                command.append(" ");
            }
            command.append(number);
            command.append(" ");
            if (method.isAnnotationPresent(CCDirectCall.class)) {
                command.append("D");
            } else {
                command.append(" ");
            }
            if (method.isAnnotationPresent(CCQueued.class)) {
                command.append("Q");
            } else {
                command.append(" ");
            }
            command.append(": ");
            command.append(method.getName());
            StringBuilder param = new StringBuilder();
            param.append("(");
            boolean a = false;
            for (Class<?> clazz : method.getParameterTypes()) {
                if (a) {
                    param.append(", ");
                }
                param.append(clazz.getSimpleName());
                a = true;
            }
            param.append(")");
            if (param.toString().length() + command.length() > 50) {
                command.append("\n      ---");
            }
            command.append(param);
            help.append(command);
        }
        String commands = help.toString();
        String[] lines = commands.split("\n");
        if (lines.length > 16) {
            int pageNumber = 1;
            if (args.count() > 0) {
                if (args.isDouble(0) || args.isInteger(0)) {
                    pageNumber = args.checkInteger(0);
                    if (pageNumber < 1) {
                        pageNumber = 1;
                    }
                }
            }
            StringBuilder page = new StringBuilder();
            page.append(head);
            page.append("Page ");
            page.append(pageNumber);
            page.append(" of ");
            page.append(lines.length / 10 + (lines.length % 10 == 0 ? 0 : 1));
            page.append("\n");
            page.append(head2);
            pageNumber--;
            int from = pageNumber * 11;
            int to = pageNumber * 11 + 11;
            for (int i = from; i < to; i++) {
                if (i < lines.length) {
                    page.append(lines[i]);
                }
                if (i < to - 1) {
                    page.append("\n");
                }
            }
            return new Object[] { page.toString() };
        } else {
            for (int i = 0; i < 16 - lines.length; i++) {
                String buffer = head.toString();
                head = new StringBuilder();
                head.append("\n").append(buffer);
            }
        }
        return new Object[] { String.valueOf(head) + head2 + help };
    }

    @Callback(direct = true)
    public Object[] helpCommand(Context context, Arguments args) throws Exception {
        if (object == null) {
            throw new Exception("This LP object is not persistable");
        }
        if (args.count() != 1) {
            return new Object[] { "Wrong Argument Count" };
        }
        if (!args.isInteger(0)) {
            return new Object[] { "Wrong Argument Type" };
        }
        Integer number = args.checkInteger(0);
        if (!info.commands.containsKey(number)) {
            return new Object[] { "No command with that index" };
        }
        Method method = info.commands.get(number);
        StringBuilder help = new StringBuilder();
        help.append("---------------------------------\n");
        help.append("Command: ");
        help.append(method.getName());
        help.append("\n");
        help.append("Parameter: ");
        if (method.getParameterCount() > 0) {
            help.append("\n");
            boolean a = false;
            for (Class<?> clazz : method.getParameterTypes()) {
                if (a) {
                    help.append(", ");
                }
                help.append(clazz.getSimpleName());
                a = true;
            }
            help.append("\n");
        } else {
            help.append("NONE\n");
        }
        help.append("Return Type: ");
        help.append(method.getReturnType().getName());
        help.append("\n");
        help.append("Description: \n");
        help.append(method.getAnnotation(CCCommand.class).description());
        return new Object[] { help.toString() };
    }

    @Override
    public String toString() {
        if (object != null) {
            try {
                if (object.getClass().getMethod("toString").getDeclaringClass() != Object.class) {
                    return getType() + ": " + object.toString();
                }
                if (object instanceof ICCTypeWrapped) {
                    if (((ICCTypeWrapped) object).getObject().getClass().getMethod("toString").getDeclaringClass()
                            != Object.class) {
                        return getType() + ": " + ((ICCTypeWrapped) object).getObject().toString();
                    }
                }
            } catch (NoSuchMethodException | SecurityException e) {
                if (LPConstants.DEBUG) {
                    e.printStackTrace();
                }
            }
        }
        return getType();
    }

    public Object[] invokeMethod(String methodName, Context context, Arguments args) throws Exception {
        if (object == null) {
            throw new Exception("This LP object is not persistable");
        }

        int length = args.count();
        Object[] arguments = new Object[length];
        for (int i = 0; i < length; i++) {
            if (args.isString(i)) {
                arguments[i] = args.checkString(i);
            } else {
                Object tmp = args.checkAny(i);
                if (tmp instanceof BaseWrapperClass) {
                    tmp = ((BaseWrapperClass) tmp).getObject();
                }
                if (tmp instanceof ICCTypeWrapped) {
                    tmp = ((ICCTypeWrapped) tmp).getObject();
                }
                arguments[i] = tmp;
            }
        }

        Method match = null;
        for (Method method : info.commands.values()) {
            if (!method.getName().equalsIgnoreCase(methodName)) {
                continue;
            }
            if (!argumentsMatch(method, arguments)) {
                continue;
            }
            match = method;
            break;
        }

        if (match == null) {
            StringBuilder error = new StringBuilder();

            // Build argument type list for the attempted call
            StringBuilder attemptedArgs = new StringBuilder();
            attemptedArgs.append("(");
            for (int i = 0; i < arguments.length; i++) {
                if (i > 0) {
                    attemptedArgs.append(", ");
                }
                attemptedArgs.append(arguments[i] != null ? arguments[i].getClass().getSimpleName() : "null");
            }
            attemptedArgs.append(")");

            error.append("Method '").append(methodName).append(attemptedArgs).append("' not found.");

            // Check if any methods with this name exist
            boolean foundMethodsWithSameName = false;
            for (Method method : info.commands.values()) {
                if (method.getName().equalsIgnoreCase(methodName)) {
                    foundMethodsWithSameName = true;
                    break;
                }
            }

            if (foundMethodsWithSameName) {
                error.append("\n\nAvailable overloads for '").append(methodName).append("':");
                for (Method method : info.commands.values()) {
                    if (!method.getName().equalsIgnoreCase(methodName)) {
                        continue;
                    }
                    error.append("\n  - ").append(method.getName()).append("(");
                    boolean first = true;
                    for (Class<?> clazz : method.getParameterTypes()) {
                        if (!first) {
                            error.append(", ");
                        }
                        error.append(clazz.getSimpleName());
                        first = false;
                    }
                    error.append(")");
                }
                error.append("\n\nCheck parameter types and count.");
            } else {
                error.append("\n\nNo method named '").append(methodName).append("' exists.");
                error.append("\nUse help() to see all available methods.");
            }

            throw new UnsupportedOperationException(error.toString());
        }

        if (match.getAnnotation(CCDirectCall.class) != null) {
            if (!isDirectCall) {
                throw new PermissionException();
            }
        }

        if (match.getAnnotation(CCCommand.class).needPermission()) {
            if (info.securityMethod != null) {
                try {
                    info.securityMethod.invoke(object);
                } catch (InvocationTargetException e) {
                    if (e.getTargetException() instanceof Exception) {
                        throw (Exception) e.getTargetException();
                    }
                    throw e;
                }
            }
        }

        Object result;
        try {
            if (info.setSourceMod != null)
                info.setSourceMod.invoke(object, CCWrapperInformation.SourceMod.OPENCOMPUTERS);
            result = match.invoke(object, arguments);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof Exception) {
                throw (Exception) e.getTargetException();
            }
            throw e;
        }
        return CCObjectWrapper.createArray(CCObjectWrapper.getWrappedObject(result, BaseWrapperClass.WRAPPER));
    }

    private boolean argumentsMatch(Method method, Object[] arguments) {
        if (arguments.length != method.getParameterCount()) return false; // he must not skip loop for empty parameters
                                                                          // method and not empty arguments
        int i = 0;
        for (Class<?> args : method.getParameterTypes()) {
            if (arguments.length <= i) {
                return false;
            }
            if (!args.isAssignableFrom(arguments[i].getClass())) {
                return false;
            }
            i++;
        }
        return true;
    }

    public String getType() {
        return info.type;
    }

    public Object getObject() {
        return object;
    }

    @Override
    public void load(NBTTagCompound nbt) {
        if (object != null) {
            return;
        }
        String type = nbt.getString("Type");
        if (type.equals("")) {
            return;
        }
        if ("LPGlobalCCAccess".equals(type)) {
            object = LogisticsPipes.getComputerLP();
            checkType();
        } else if ("CoreRoutedPipe".equals(type)) {
            int x = nbt.getInteger("X");
            int y = nbt.getInteger("Y");
            int z = nbt.getInteger("Z");
            final LPPosition pos = new LPPosition(x, y, z);
            final int dim = nbt.getInteger("Dim");
            QueuedTasks.queueTask((Callable<Object>) () -> {
                World world = DimensionManager.getWorld(dim);
                if (world != null) {
                    TileEntity tile = pos.getTileEntity(world);
                    if (tile instanceof LogisticsTileGenericPipe
                            && ((LogisticsTileGenericPipe) tile).pipe instanceof CoreRoutedPipe) {
                        object = ((LogisticsTileGenericPipe) tile).pipe;
                        checkType();
                    }
                }
                return null;
            });
        } else if ("CCItemIdentifierImplementation".equals(type)) {
            ItemStack stack = ItemStack.loadItemStackFromNBT(nbt);
            if (stack != null) {
                object = new CCItemIdentifierImplementation(ItemIdentifier.get(stack));
                checkType();
            }
        } else if ("CCItemIdentifierStackImplementation".equals(type)) {
            ItemStack stack = ItemStack.loadItemStackFromNBT(nbt);
            if (stack != null) {
                object = new CCItemIdentifierStackImplementation(ItemIdentifierStack.getFromStack(stack));
                checkType();
            }
        } else if ("CCItemIdentifierBuilder".equals(type)) {
            ItemStack stack = ItemStack.loadItemStackFromNBT(nbt);
            if (stack != null) {
                CCItemIdentifierBuilder builder = new CCItemIdentifierBuilder();
                builder.setItemID(Long.valueOf(Item.getIdFromItem(stack.getItem())));
                builder.setItemData((long) stack.getItemDamage());
                object = builder;
                checkType();
            }
        } else if ("LogisticsSolidTileEntity".equals(type)) {
            int x = nbt.getInteger("X");
            int y = nbt.getInteger("Y");
            int z = nbt.getInteger("Z");
            final LPPosition pos = new LPPosition(x, y, z);
            final int dim = nbt.getInteger("Dim");
            QueuedTasks.queueTask((Callable<Object>) () -> {
                World world = DimensionManager.getWorld(dim);
                if (world != null) {
                    TileEntity tile = pos.getTileEntity(world);
                    if (tile instanceof LogisticsSolidTileEntity) {
                        object = tile;
                        checkType();
                    }
                }
                return null;
            });
        } else {
            System.out.println("Unknown type to load");
        }
    }

    @Override
    public void save(NBTTagCompound nbt) {
        if (object == null) {
            return;
        }
        if (object instanceof LPGlobalCCAccess) {
            nbt.setString("Type", "LPGlobalCCAccess");
        } else if (object instanceof CoreRoutedPipe) {
            LPPosition pos = ((CoreRoutedPipe) object).getLPPosition();
            nbt.setString("Type", "CoreRoutedPipe");
            nbt.setInteger("Dim", MainProxy.getDimensionForWorld(((CoreRoutedPipe) object).getWorld()));
            nbt.setInteger("X", pos.getX());
            nbt.setInteger("Y", pos.getY());
            nbt.setInteger("Z", pos.getZ());
        } else if (object instanceof CCItemIdentifierImplementation) {
            nbt.setString("Type", "CCItemIdentifierImplementation");
            ((CCItemIdentifierImplementation) object).getObject().makeNormalStack(1).writeToNBT(nbt);
        } else if (object instanceof CCItemIdentifierStackImplementation) {
            nbt.setString("Type", "CCItemIdentifierStackImplementation");
            ((CCItemIdentifierStackImplementation) object).getObject().makeNormalStack().writeToNBT(nbt);
        } else if (object instanceof CCItemIdentifierBuilder) {
            nbt.setString("Type", "CCItemIdentifierBuilder");
            ((CCItemIdentifierBuilder) object).build().makeNormalStack(1).writeToNBT(nbt);
        } else if (object instanceof LogisticsSolidTileEntity) {
            LPPosition pos = ((LogisticsSolidTileEntity) object).getLPPosition();
            nbt.setString("Type", "LogisticsSolidTileEntity");
            nbt.setInteger("Dim", MainProxy.getDimensionForWorld(((LogisticsSolidTileEntity) object).getWorldObj()));
            nbt.setInteger("X", pos.getX());
            nbt.setInteger("Y", pos.getY());
            nbt.setInteger("Z", pos.getZ());
        } else {
            System.out.println("Couldn't find mapping for: " + object.getClass());
        }
    }

    private void checkType() {
        if (object != null) {
            if (CCObjectWrapper.getWrapperInformation(object.getClass()) != info) {
                System.out.println("WrapperInformationTypes didn't match");
                object = null;
            }
        }
    }

    @Override
    public Object[] call(Context context, Arguments arguments) {
        try {
            return help(context, arguments);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
