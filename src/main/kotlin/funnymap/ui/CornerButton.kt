package funnymap.ui

import funnymap.utils.RenderUtils
import java.awt.Color

class CornerButton(var x: Double, var y: Double, val corner: Corner) {
    fun isHovered(mouseX: Int, mouseY: Int) = mouseX.toDouble() in x..x + 6.0 && mouseY.toDouble() in y..y + 6.0

    fun draw() {
        RenderUtils.renderRect(x, y, 6.0, 6.0, Color(255, 255, 255))
    }

    enum class Corner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }
}
