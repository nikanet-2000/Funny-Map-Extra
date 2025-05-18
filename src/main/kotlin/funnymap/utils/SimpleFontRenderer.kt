package funnymap.utils

import funnymap.FunnyMap.mc
import funnymap.utils.RenderUtilsGL.bind
import net.minecraft.client.renderer.texture.SimpleTexture
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraft.client.resources.IResourceManager
import net.minecraft.client.resources.IResourceManagerReloadListener
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.IOException
import kotlin.math.max

object SimpleFontRenderer : IResourceManagerReloadListener {
    private val locationFontTexture = ResourceLocation("textures/font/ascii.png")
    private var charWidth: IntArray = IntArray(256)
    private var posX: Float = 0f
    private var posY: Float = 0f
    const val FONT_HEIGHT: Int = 9
    private const val VALID_CHARS =
        "\u00c0\u00c1\u00c2\u00c8\u00ca\u00cb\u00cd\u00d3\u00d4\u00d5\u00da\u00df\u00e3\u00f5\u011f\u0130\u0131\u0152\u0153\u015e\u015f\u0174\u0175\u017e\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580\u03b1\u03b2\u0393\u03c0\u03a3\u03c3\u03bc\u03c4\u03a6\u0398\u03a9\u03b4\u221e\u2205\u2208\u2229\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0\u0000"

    init {
        readFontTexture()
    }

    override fun onResourceManagerReload(resourceManager: IResourceManager) {
        readFontTexture()
    }

    private fun readFontTexture() {
        val bufferedImage: BufferedImage = try {
            TextureUtil.readBufferedImage(mc.resourceManager.getResource(locationFontTexture).inputStream)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        val w = bufferedImage.width
        val h = bufferedImage.height
        val arr = IntArray(w * h)
        bufferedImage.getRGB(0, 0, w, h, arr, 0, w)
        val charHeight = h / 16
        val charWidth = w / 16
        val scale = 8.0f / charWidth.toFloat()
        charIndex@ for (charIndex in 0..255) {
            if (charIndex == 32) {
                this.charWidth[charIndex] = 4
            }
            val column = charIndex % 16
            val row = charIndex / 16
            for (scanX in charWidth - 1 downTo 0) {
                val x = column * charWidth + scanX
                var found = false
                for (scanY in 0 until charHeight) {
                    val y = row * charHeight + scanY
                    if ((arr[x + y * w] shr 24 and 0xFF) != 0) {
                        found = true
                        break
                    }
                }
                if (found) {
                    this.charWidth[charIndex] = (0.5 + (scanX + 1) * scale).toInt() + 1
                    continue@charIndex
                }
            }
            this.charWidth[charIndex] = (0.5 + scale).toInt() + 1
        }

        if (mc.textureManager.getTexture(locationFontTexture) == null) {
            mc.textureManager.loadTexture(locationFontTexture, SimpleTexture(locationFontTexture))
        }
    }

    private fun renderChar(ch: Char): Int {
        if (ch == ' ') return 4
        val i = VALID_CHARS.indexOf(ch)
        return if (i != -1) this.renderDefaultChar(i) else 0
    }

    private fun renderDefaultChar(ch: Int): Int {
        val texX = (ch % 16 * 8) / 128.0f
        val texY = (ch / 16 * 8) / 128.0f
        val width = charWidth[ch] - 1.01f
        val height = 7.99f
        val texWidth = width / 128.0f
        val texHeight = height / 128.0f

        GL11.glBegin(GL11.GL_TRIANGLE_STRIP)
        GL11.glTexCoord2f(texX, texY)
        GL11.glVertex3f(posX, posY, 0.0f)
        GL11.glTexCoord2f(texX, texY + texHeight)
        GL11.glVertex3f(posX, posY + height, 0.0f)
        GL11.glTexCoord2f(texX + texWidth, texY)
        GL11.glVertex3f(posX + width, posY, 0.0f)
        GL11.glTexCoord2f(texX + texWidth, texY + texHeight)
        GL11.glVertex3f(posX + width, posY + height, 0.0f)
        GL11.glEnd()

        return charWidth[ch]
    }

    fun drawString(text: String, x: Float, y: Float, color: Color, dropShadow: Boolean): Int {
        var i: Int
        GL11.glEnable(GL11.GL_ALPHA_TEST)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        locationFontTexture.bind()

        if (dropShadow) {
            i = renderString(text, x + 1.0f, y + 1.0f, color, true)
            i = max(i, renderString(text, x, y, color, false))
        } else {
            i = renderString(text, x, y, color, false)
        }

        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
        return i
    }

    private fun renderStringAtPos(text: String) {
        text.forEach {
            posX += renderChar(it)
        }
    }

    private fun renderString(text: String, x: Float, y: Float, color: Color, dropShadow: Boolean): Int {
        if (dropShadow) {
            GL11.glColor4f(
                color.red * .25f / 255f,
                color.green * .25f / 255f,
                color.blue * .25f / 255f,
                color.alpha / 255f
            )
        } else {
            GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
        }
        posX = x
        posY = y
        renderStringAtPos(text)
        return posX.toInt()
    }

    fun getStringWidth(text: String): Int {
        return text.sumOf { getCharWidth(it) }
    }

    fun getCharWidth(character: Char): Int {
        if (character == ' ') return 4
        val i = VALID_CHARS.indexOf(character)
        if (character > '\u0000' && i != -1) {
            return charWidth[i]
        }
        return 0
    }
}
