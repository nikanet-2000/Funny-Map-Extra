package funnymap.ui

import funnymap.FunnyMap.mc
import funnymap.utils.RenderUtils
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Keyboard
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

abstract class MovableGuiElement {

    val corners = arrayOf(
        CornerButton(0.0, 0.0, CornerButton.Corner.TOP_LEFT),
        CornerButton(0.0, 0.0, CornerButton.Corner.TOP_RIGHT),
        CornerButton(0.0, 0.0, CornerButton.Corner.BOTTOM_LEFT),
        CornerButton(0.0, 0.0, CornerButton.Corner.BOTTOM_RIGHT)
    )
    abstract var x: Int
    abstract var y: Int
    abstract var x2: Int
    abstract var y2: Int
    abstract val w: Int
    abstract val h: Int
    open var scale: Float = 1f

    open fun draw(mouseX: Int, mouseY: Int) {
        GlStateManager.pushMatrix()
        GlStateManager.translate(x.toFloat(), y.toFloat(), 0f)
        GlStateManager.scale(scale, scale, 1f)
        render()
        GlStateManager.popMatrix()

        RenderUtils.renderRectBorder(
            x.toDouble(),
            y.toDouble(),
            w * scale.toDouble(),
            h * scale.toDouble(),
            0.5,
            Color(255, 255, 255)
        )
        updateCorners()
        corners.forEach { it.draw() }
    }

    open fun render() {}

    open fun shouldRender(): Boolean = true

    fun isHovered(mouseX: Int, mouseY: Int) = mouseX in x..x2 && mouseY in y..y2

    private fun updateCorners() {
        corners[0].x = x - 3.0
        corners[0].y = y - 3.0
        corners[1].x = x2 - 3.0
        corners[1].y = y - 3.0
        corners[2].x = x - 3.0
        corners[2].y = y2 - 3.0
        corners[3].x = x2 - 3.0
        corners[3].y = y2 - 3.0
    }

    fun setLocation(x: Int, y: Int) {
        this.x = x.coerceIn(0, ((mc.displayWidth - w * scale).toInt()))
        this.y = y.coerceIn(0, (mc.displayHeight - h * scale).toInt())
        x2 = (x + w * scale).toInt()
        y2 = (y + h * scale).toInt()
    }

    fun mouseScroll(direction: Int) {
        if (direction != 0) {
            var increment = direction * 0.01f
            if (!GuiScreen.isShiftKeyDown()) increment *= 5
            scale = (scale + increment).coerceAtLeast(0.1f)
            x2 = (x + w * scale).toInt()
            y2 = (y + h * scale).toInt()
        }
    }

    fun cornerDrag(mouseX: Int, mouseY: Int, corner: CornerButton.Corner) {
        val maxScaleLeft = x2 / w.toFloat()
        val maxScaleTop = y2 / h.toFloat()
        val maxScaleRight = (mc.displayWidth - x) / w.toFloat()
        val maxScaleBottom = (mc.displayHeight - y) / h.toFloat()
        when (corner) {
            CornerButton.Corner.TOP_LEFT -> {
                var s = max((x2 - mouseX) / w.toFloat(), (y2 - mouseY) / h.toFloat())
                    .coerceIn(0.1f..min(maxScaleTop, maxScaleLeft))
                s = (s * 100).toInt() / 100f
                x = (x2 - w * s).toInt()
                y = (y2 - h * s).toInt()
                scale = s
            }

            CornerButton.Corner.TOP_RIGHT -> {
                var s = max((mouseX - x) / w.toFloat(), (y2 - mouseY) / h.toFloat())
                    .coerceIn(0.1f..min(maxScaleTop, maxScaleRight))
                s = (s * 100).toInt() / 100f
                x2 = (x + w * s).toInt()
                y = (y2 - h * s).toInt()
                scale = s
            }

            CornerButton.Corner.BOTTOM_LEFT -> {
                var s = max((x2 - mouseX) / w.toFloat(), (mouseY - y) / h.toFloat())
                    .coerceIn(0.1f..min(maxScaleBottom, maxScaleLeft))
                s = (s * 100).toInt() / 100f
                x = (x2 - w * s).toInt()
                y2 = (y + h * s).toInt()
                scale = s
            }

            CornerButton.Corner.BOTTOM_RIGHT -> {
                var s = max((mouseX - x) / w.toFloat(), (mouseY - y) / h.toFloat())
                    .coerceIn(0.1f..min(maxScaleBottom, maxScaleRight))
                s = (s * 100).toInt() / 100f
                x2 = (x + w * s).toInt()
                y2 = (y + h * s).toInt()
                scale = s
            }
        }
    }

    fun keyTyped(keyCode: Int) {
        val increment = if (GuiScreen.isShiftKeyDown()) 5 else 1
        when (keyCode) {
            Keyboard.KEY_LEFT -> setLocation(x - increment, y)
            Keyboard.KEY_RIGHT -> setLocation(x + increment, y)
            Keyboard.KEY_UP -> setLocation(x, y - increment)
            Keyboard.KEY_DOWN -> setLocation(x, y + increment)
        }
    }
}
