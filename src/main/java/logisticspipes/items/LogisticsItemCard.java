package logisticspipes.items;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import logisticspipes.interfaces.IItemAdvancedExistance;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.string.StringUtils;

public class LogisticsItemCard extends LogisticsItem implements IItemAdvancedExistance {

    public static final int FREQ_CARD = 0;
    public static final int SEC_CARD = 1;

    public LogisticsItemCard() {
        hasSubtypes = true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack itemStack, EntityPlayer player, List<String> list, boolean flag) {
        super.addInformation(itemStack, player, list, flag);
        if (!itemStack.hasTagCompound()) {
            list.add(StringUtils.translate("tooltip.logisticsItemCard"));
        } else {
            if (itemStack.getTagCompound().hasKey("UUID")) {
                if (itemStack.getItemDamage() == LogisticsItemCard.FREQ_CARD) {
                    list.add("Freq. Card");
                } else if (itemStack.getItemDamage() == LogisticsItemCard.SEC_CARD) {
                    list.add("Sec. Card");
                }
                if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                    list.add("Id: " + itemStack.getTagCompound().getString("UUID"));
                    if (itemStack.getItemDamage() == LogisticsItemCard.SEC_CARD) {
                        UUID id = UUID.fromString(itemStack.getTagCompound().getString("UUID"));
                        list.add(
                                "Authorization: "
                                        + (SimpleServiceLocator.securityStationManager.isAuthorized(id) ? "Authorized"
                                                : "Deauthorized"));
                    }
                }
            }
        }
    }

    @Override
    public boolean getShareTag() {
        return true;
    }

    @Override
    public int getItemStackLimit() {
        return 64;
    }

    @Override
    public boolean canExistInNormalInventory(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canExistInWorld(ItemStack stack) {
        return stack.getItemDamage() != LogisticsItemCard.SEC_CARD;
    }
}
