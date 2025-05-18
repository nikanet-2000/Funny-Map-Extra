package funnymap.features.dungeon

import funnymap.FunnyMap.mc
import funnymap.config.Config
import funnymap.core.map.*
import funnymap.features.dungeon.MapRender.dynamicRotation
import funnymap.features.dungeon.MapRender.legitRender
import funnymap.utils.Location.inBoss
import funnymap.utils.MapUtils
import funnymap.utils.MapUtils.connectorSize
import funnymap.utils.MapUtils.halfRoomSize
import funnymap.utils.MapUtils.roomSize
import funnymap.utils.RenderUtils
import funnymap.utils.RenderUtils.darken
import funnymap.utils.RenderUtils.grayScale
import funnymap.utils.RenderUtilsGL
import funnymap.utils.Utils.equalsOneOf
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11

object MapRenderList {
    var renderUpdated = false
    private var borderGlList = -1
    private var roomGlList = -1

    fun updateRenderMap() {
        if (borderGlList == -1) {
            borderGlList = GL11.glGenLists(1)
            GL11.glNewList(borderGlList, GL11.GL_COMPILE)
            RenderUtilsGL.renderRect(
                0.0, 0.0, 128.0, if (Config.mapShowRunInformation) 142.0 else 128.0, Config.mapBackground
            )
            RenderUtilsGL.renderRectBorder(
                0.0,
                0.0,
                128.0,
                if (Config.mapShowRunInformation) 142.0 else 128.0,
                Config.mapBorderWidth.toDouble(),
                Config.mapBorder
            )
            GL11.glEndList()
        }

        if (renderUpdated && Config.renderBeta) {
            if (roomGlList >= 0) {
                GL11.glDeleteLists(roomGlList, 1)
                roomGlList = -1
            }
            roomGlList = GL11.glGenLists(1)
            renderUpdated = false

            GL11.glNewList(roomGlList, GL11.GL_COMPILE)
            renderRooms()
            renderText()
            GL11.glEndList()

        }
    }

    fun renderMap() {
        if (roomGlList == -1 || borderGlList == -1 || renderUpdated) {
            updateRenderMap()
        }

        GlStateManager.pushMatrix()
        RenderUtils.preDraw()
        RenderUtilsGL.preDraw()

        mc.mcProfiler.startSection("border")

        if (borderGlList != -1) GL11.glCallList(borderGlList)

        mc.mcProfiler.endSection()

        if (Config.mapRotate) {
            GlStateManager.pushMatrix()
            MapRender.setupRotate()
        } else if (Config.mapDynamicRotate) {
            GlStateManager.translate(64.0, 64.0, 0.0)
            GlStateManager.rotate(dynamicRotation, 0f, 0f, 1f)
            GlStateManager.translate(-64.0, -64.0, 0.0)
        }

        mc.mcProfiler.startSection("rooms")

        if (roomGlList != -1) GL11.glCallList(roomGlList)

        RenderUtilsGL.unbindTexture()
        RenderUtils.postDraw()
        RenderUtilsGL.postDraw()
        GlStateManager.popMatrix()

        if (!inBoss) {
            mc.mcProfiler.endStartSection("heads")
            MapRender.renderPlayerHeads()
        }

        mc.mcProfiler.endSection()

        if (Config.mapRotate) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST)
            GlStateManager.popMatrix()
        } else if (Config.mapDynamicRotate) {
            GlStateManager.translate(64.0, 64.0, 0.0)
            GlStateManager.rotate(-dynamicRotation, 0f, 0f, 1f)
            GlStateManager.translate(-64.0, -64.0, 0.0)
        }

        if (Config.mapShowRunInformation) {
            mc.mcProfiler.startSection("footer")
            MapRender.renderRunInformation()
            mc.mcProfiler.endSection()
        }
    }

    private fun renderRooms() {
        GlStateManager.translate(MapUtils.startCorner.first.toFloat(), MapUtils.startCorner.second.toFloat(), 0f)

        var yPos = 0
        var yStep = 0

        for (y in 0..10) {
            val yEven = y % 2 == 0
            yPos += yStep
            yStep = if (yEven) roomSize else connectorSize
            var xPos = 0
            var xStep = 0
            for (x in 0..10) {
                val xEven = x % 2 == 0
                xPos += xStep
                xStep = if (xEven) roomSize else connectorSize

                val tile = Dungeon.Info.dungeonList[y * 11 + x]
                if (tile is Unknown) continue
                if (legitRender && tile.state == RoomState.UNDISCOVERED) continue

                var color = tile.color

                if (tile.state.equalsOneOf(RoomState.UNDISCOVERED, RoomState.UNOPENED) &&
                    !legitRender && Dungeon.Info.startTime != 0L
                ) {
                    if (Config.mapDarkenUndiscovered) {
                        color = color.darken(1 - Config.mapDarkenPercent)
                    }
                    if (Config.mapGrayUndiscovered) {
                        color = color.grayScale()
                    }
                }

                when (tile) {
                    is Room -> {
                        RenderUtilsGL.renderRect(xPos, yPos, xStep, yStep, color)
                        if (legitRender && tile.state == RoomState.UNOPENED) {
                            RenderUtilsGL.drawCheckmark(xPos.toFloat(), yPos.toFloat(), tile.state)
                        }
                    }

                    is Door -> {
                        val doorOffset = if (roomSize == 16) 5 else 6
                        if (xEven) {
                            RenderUtilsGL.renderRect(xPos + doorOffset, yPos, xStep - doorOffset * 2, yStep, color)
                        } else {
                            RenderUtilsGL.renderRect(xPos, yPos + doorOffset, xStep, yStep - doorOffset * 2, color)
                        }
                    }
                }
            }
        }
        GlStateManager.translate(-MapUtils.startCorner.first.toFloat(), -MapUtils.startCorner.second.toFloat(), 0f)
    }

    private fun renderText() {
        GlStateManager.translate(MapUtils.startCorner.first.toFloat(), MapUtils.startCorner.second.toFloat(), 0f)

        Dungeon.Info.uniqueRooms.forEach { unique ->
            val room = unique.mainRoom
            if (legitRender && room.state.equalsOneOf(RoomState.UNDISCOVERED, RoomState.UNOPENED)) return@forEach
            val checkPos = unique.getCheckmarkPosition()
            val namePos = unique.getNamePosition()
            val xPosCheck = (checkPos.first / 2f) * (roomSize + connectorSize)
            val yPosCheck = (checkPos.second / 2f) * (roomSize + connectorSize)
            val xPosName = (namePos.first / 2f) * (roomSize + connectorSize)
            val yPosName = (namePos.second / 2f) * (roomSize + connectorSize)

            if (Config.mapCheckmark != 0 && Config.mapRoomSecrets != 2) {
                RenderUtilsGL.drawCheckmark(xPosCheck, yPosCheck, room.state)
            }

            val color = if (Config.mapColorText) when (room.state) {
                RoomState.GREEN -> Config.colorTextGreen
                RoomState.CLEARED -> Config.colorTextCleared
                RoomState.FAILED -> Config.colorTextFailed
                else -> Config.colorTextUncleared
            } else Config.colorTextCleared

            if (Config.mapRoomSecrets == 2) {
                GlStateManager.pushMatrix()
                GlStateManager.translate(
                    xPosCheck + halfRoomSize, yPosCheck + 2 + halfRoomSize, 0f
                )
                GlStateManager.scale(2f, 2f, 1f)
                RenderUtilsGL.renderCenteredText(listOf(room.data.secrets.toString()), 0, 0, color)
                GlStateManager.popMatrix()
            }

            val name = mutableListOf<String>()

            if (Config.mapRoomNames != 0 && room.data.type.equalsOneOf(
                    RoomType.PUZZLE,
                    RoomType.TRAP
                ) || Config.mapRoomNames == 2 && room.data.type.equalsOneOf(
                    RoomType.NORMAL, RoomType.RARE, RoomType.CHAMPION
                )
            ) {
                name.addAll(room.data.name.split(" "))
            }
            if (room.data.type == RoomType.NORMAL && Config.mapRoomSecrets == 1) {
                name.add(room.data.secrets.toString())
            }
            // Offset + half of roomsize
            RenderUtilsGL.renderCenteredText(
                name,
                xPosName.toInt() + halfRoomSize,
                yPosName.toInt() + halfRoomSize,
                color
            )
        }
        GlStateManager.translate(-MapUtils.startCorner.first.toFloat(), -MapUtils.startCorner.second.toFloat(), 0f)
    }
}
