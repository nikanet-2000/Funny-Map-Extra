package funnymap.ui

import funnymap.config.Config
import funnymap.ui.GuiRenderer.elements
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

class EditLocationGui : GuiScreen() {

    private var hovered: MovableGuiElement? = null
    private var startOffsetX = 0
    private var startOffsetY = 0
    private var isDragging = false
    private var resizingCorner: CornerButton? = null

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        super.initGui()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        this.drawDefaultBackground()
        elements.forEach {
            it.draw(mouseX, mouseY)
        }
        if (!isDragging && resizingCorner == null) {
            hovered = elements.find { it.isHovered(mouseX, mouseY) }
        }
        mouseDrag(mouseX, mouseY)
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    fun mouseDrag(mouseX: Int, mouseY: Int) {
        if (hovered == null) return
        if (isDragging) {
            hovered?.setLocation((mouseX - startOffsetX), (mouseY - startOffsetY))
        } else {
            resizingCorner?.corner?.let {
                hovered?.cornerDrag(mouseX, mouseY, it)
            }
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (mouseButton == 0) {
            resizingCorner = null
            elements.firstOrNull { guiElement ->
                resizingCorner = guiElement.corners.find { it.isHovered(mouseX, mouseY) }
                resizingCorner != null
            }?.let { hovered = it }

            if (resizingCorner == null) {
                hovered?.let {
                    startOffsetX = (mouseX - it.x)
                    startOffsetY = (mouseY - it.y)
                    isDragging = true
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        isDragging = false
        resizingCorner = null
        super.mouseReleased(mouseX, mouseY, state)
    }

    override fun handleMouseInput() {
        val i = Mouse.getEventDWheel().coerceIn(-1..1)
        hovered?.mouseScroll(i)
        super.handleMouseInput()
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        hovered?.keyTyped(keyCode)
        super.keyTyped(typedChar, keyCode)
    }

    override fun onGuiClosed() {
        Keyboard.enableRepeatEvents(false)
        Config.markDirty()
        Config.writeData()
        super.onGuiClosed()
    }
}
