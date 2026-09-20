package docscooby.shapeshifter.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ShapeshifterListScreen extends Screen {
    private final List<String> names;

    public ShapeshifterListScreen(List<String> names) {
        super(Component.literal("Stolen Skins"));
        this.names = names;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Close"), button -> this.onClose())
                .bounds(this.width / 2 - 50, this.height - 30, 100, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);

        if (names.isEmpty()) {
            graphics.centeredText(this.font, Component.literal("No stolen skins yet."), this.width / 2, 50, 0xFFAAAAAA);
            return;
        }

        int y = 50;
        for (String name : names) {
            graphics.centeredText(this.font, Component.literal(name), this.width / 2, y, 0xFFFFFFFF);
            y += 12;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}