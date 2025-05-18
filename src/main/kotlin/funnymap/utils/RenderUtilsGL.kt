package funnymap.utils

import funnymap.FunnyMap.mc
import funnymap.config.Config
import funnymap.core.map.RoomState
import funnymap.features.dungeon.MapRender
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.WorldRenderer
import net.minecraft.client.renderer.texture.SimpleTexture
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.GL_QUADS
import java.awt.Color

object RenderUtilsGL {

    private val tessellator: Tessellator = Tessellator.getInstance()
    private val worldRenderer: WorldRenderer = tessellator.worldRenderer
    private var currentTexture = 0

    fun preDraw() {
        GL11.glEnable(GL11.GL_ALPHA_TEST)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LIGHTING)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
    }

    fun postDraw() {
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
    }

    fun renderRect(x: Number, y: Number, w: Number, h: Number, color: Color) {
        if (color.alpha == 0) return
        preDraw()
        color.bind()

        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
        RenderUtils.addQuadVertices(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble())
        tessellator.draw()

        postDraw()
    }

    fun renderRectBorder(x: Double, y: Double, w: Double, h: Double, thickness: Double, color: Color) {
        if (color.alpha == 0) return
        preDraw()
        color.bind()

        worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
        RenderUtils.addQuadVertices(x - thickness, y, thickness, h)
        RenderUtils.addQuadVertices(x - thickness, y - thickness, w + thickness * 2, thickness)
        RenderUtils.addQuadVertices(x + w, y, thickness, h)
        RenderUtils.addQuadVertices(x - thickness, y + h, w + thickness * 2, thickness)
        tessellator.draw()

        postDraw()
    }

    fun renderCenteredText(text: List<String>, x: Int, y: Int, color: Color) {
        if (text.isEmpty()) return
        GlStateManager.pushMatrix()
        GlStateManager.translate(x.toFloat(), y.toFloat(), 0f)
        GlStateManager.scale(Config.textScale, Config.textScale, 1f)

        if (Config.mapRotate) {
            GlStateManager.rotate(mc.thePlayer.rotationYaw + 180f, 0f, 0f, 1f)
        } else if (Config.mapDynamicRotate) {
            GlStateManager.rotate(-MapRender.dynamicRotation, 0f, 0f, 1f)
        }

        val fontHeight = SimpleFontRenderer.FONT_HEIGHT + 1
        val yTextOffset = text.size * fontHeight / -2f

        text.withIndex().forEach { (index, text) ->
            SimpleFontRenderer.drawString(
                text,
                SimpleFontRenderer.getStringWidth(text) / -2f,
                yTextOffset + index * fontHeight,
                color,
                true
            )
        }

        GlStateManager.popMatrix()
    }

    fun drawCheckmark(x: Float, y: Float, state: RoomState) {
        val (checkmark, size) = when (Config.mapCheckmark) {
            1 -> RenderUtils.defaultCheckmarks.getCheckmark(state) to RenderUtils.defaultCheckmarks.size.toDouble()
            2 -> RenderUtils.neuCheckmarks.getCheckmark(state) to RenderUtils.neuCheckmarks.size.toDouble()
            3 -> RenderUtils.legacyCheckmarks.getCheckmark(state) to RenderUtils.legacyCheckmarks.size.toDouble()
            else -> return
        }
        if (checkmark != null) {
            GL11.glColor4f(1f, 1f, 1f, 1f)
            GL11.glEnable(GL11.GL_ALPHA_TEST)
            GL11.glEnable(GL11.GL_TEXTURE_2D)
            checkmark.bind()

            RenderUtils.drawTexturedQuad(
                x + (MapUtils.roomSize - size) / 2,
                y + (MapUtils.roomSize - size) / 2,
                size,
                size
            )
        }
    }

    private fun Color.bind() {
        GL11.glColor4f(this.red / 255f, this.green / 255f, this.blue / 255f, this.alpha / 255f)
    }

    fun ResourceLocation.bind() {
        var tex = mc.textureManager.getTexture(this)
        if (tex == null) {
            tex = SimpleTexture(this)
            mc.textureManager.loadTexture(this, tex)
        }
        if (tex.glTextureId != currentTexture) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex.glTextureId)
            currentTexture = tex.glTextureId
        }
    }

    fun unbindTexture() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
        currentTexture = 0
    }
}
