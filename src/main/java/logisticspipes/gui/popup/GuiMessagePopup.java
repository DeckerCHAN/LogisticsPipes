package logisticspipes.gui.popup;

import net.minecraft.client.gui.GuiButton;

import logisticspipes.utils.gui.GuiGraphics;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.string.StringUtils;

public class GuiMessagePopup extends SubGuiScreen {

    private final String[] text;
    private int mWidth = 0;

    public GuiMessagePopup(Object... message) {
        super(200, (message.length * 10) + 40, 0, 0);
        text = new String[message.length];
        int i = 0;
        for (Object o : message) {
            if (o instanceof Object[]) {
                for (Object oZwei : (Object[]) o) {
                    text[i++] = oZwei.toString();
                }
            } else {
                text[i++] = o.toString();
            }
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        buttonList.add(new GuiButton(0, xCenter - 25, bottom - 25, 50, 20, "OK"));
    }

    @Override
    protected void renderGuiBackground(int par1, int par2) {
        if (mWidth == 0) {
            int lWidth = 0;
            for (String msg : text) {
                int tWidth = mc.fontRenderer.getStringWidth(msg);
                if (tWidth > lWidth) {
                    lWidth = tWidth;
                }
            }
            xSize = mWidth = Math.max(Math.min(lWidth + 20, 400), 120);
            super.initGui();
        }
        GuiGraphics.drawGuiBackGround(mc, guiLeft, guiTop, right, bottom, zLevel, true);
        for (int i = 0; i < 9 && i < text.length; i++) {
            if (text[i] == null) {
                continue;
            }
            String msg = StringUtils.getCuttedString(text[i], mWidth - 10, mc.fontRenderer);
            int stringWidth = mc.fontRenderer.getStringWidth(msg);
            mc.fontRenderer.drawString(msg, xCenter - (stringWidth / 2), guiTop + 10 + (i * 10), 0x404040);
        }
    }

    @Override
    protected void actionPerformed(GuiButton guibutton) {
        if (guibutton.id == 0) {
            super.exitGui();
        }
    }
}
