package me.stylite.predator.utils

import kotlinx.coroutines.future.await
import me.stylite.predator.models.json.UserInfo
import me.stylite.predator.models.stats.ApexProfile
import java.awt.Color
import java.awt.Font
import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO

object Imaging {

    suspend fun generateProfileCard(profile: ApexProfile, userInfo: UserInfo): ByteArray {
        return CompletableFuture.supplyAsync { generateProfileCard0(profile, userInfo) }
            .await()
    }

    private val RankPointBorder = mapOf(
        "Predetor" to 0,
        "Master" to 10000,
        "Diamond" to 7200,
        "Platinum" to 4800,
        "Gold" to 2800,
        "Silver" to 1200,
        "Bronze" to 0
    )

    private val RankDivisionPoint = mapOf(
        "Predetor" to 0,
        "Master" to 0,
        "Diamond" to 700,
        "Platinum" to 600,
        "Gold" to 500,
        "Silver" to 400,
        "Bronze" to 300
    )

    /**
     * Scales an image, preserving its aspect ratio.
     */
    private fun scale(bufferedImage: BufferedImage, width: Int): Image {
        val aspectRatio = bufferedImage.width.toDouble() / bufferedImage.height
        val newHeight = (width / aspectRatio).toInt()
        return bufferedImage.getScaledInstance(width, newHeight, bufferedImage.type)
    }

    private fun generateProfileCard0(profile: ApexProfile, userInfo: UserInfo): ByteArray {
        val baseFont = Resources.font
        // Fonts
        val font32 = baseFont.deriveFont(32f)
        val font28 = baseFont.deriveFont(28f)
        val font24 = baseFont.deriveFont(24f)
        val font20 = baseFont.deriveFont(20f)
        val font18 = baseFont.deriveFont(18f)
        // Colors
        val white = Color(255, 255, 255)
        val notQuiteWhite = Color(235, 235, 235)
        val barRed = Color(218, 41, 42)
        val blue = Color(16, 58, 200)
        val lightBlue = Color(25, 199, 200)
        val black = Color(0, 0, 0)
        val gray = Color(168, 168, 168)
        val lightGray = Color(191, 191, 191)

        val base = ImageIO.read(Resources.card)
        val gfx = base.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP)
        }

        val font32Metrics = gfx.getFontMetrics(font32)
        val font28Metrics = gfx.getFontMetrics(font28)
        val font24Metrics = gfx.getFontMetrics(font24)
        //val font18Metrics = gfx.getFontMetrics(font18)

        gfx.font = font28
        gfx.color = white

        val width = font28Metrics.stringWidth(profile.global.name)
        val nameX = 251 + (221 - width) / 2  // 292
        gfx.drawString(profile.global.name, nameX, 140 + font28Metrics.ascent)

        val barWidth = (219 * (profile.global.toNextLevelPercent.toDouble() / 100)).toInt()
        gfx.color = barRed
        gfx.fillRect(253, 183, barWidth, 31)

        gfx.font = font24
        gfx.color = black

        val barText = "Level ${profile.global.level}"
        val levelWidth = font24Metrics.stringWidth(barText)
        val levelX = 252 + (221 - levelWidth) / 2
        val glyphVector = font24.layoutGlyphVector(gfx.fontRenderContext, barText.toCharArray(), 0, barText.length, Font.LAYOUT_LEFT_TO_RIGHT)
        val levelY = 190 + glyphVector.visualBounds.height.toInt()
        gfx.drawString("Level ${profile.global.level}", levelX, levelY)

        val legendName = profile.legends.selected.LegendName
        //val legendName = legends[index++ % legends.size].capitalize() // Cycle intensifies
        val legend = Resources.legend(legendName.decapitalize())
        val legendImg = ImageIO.read(legend)

        val aspectRatio = legendImg.width.toDouble() / legendImg.height
        val newHeight = (244 / aspectRatio).toInt()
        val image = scale(legendImg, 244)

        val heightAdjust = when {
            legendName == "Revenant" -> 61
            legendName == "Loba" -> 59
            legendName == "Horizon" -> 63
            newHeight > 336 -> 78 - (newHeight - 336)
            newHeight < 336 -> 78 + (336 - newHeight)
            else -> 78
        }

        gfx.drawImage(image, 0, heightAdjust, null)

        gfx.font = font32
        gfx.color = gray

        val legendWidth = font32Metrics.stringWidth(legendName)
        val legendNameX = 11 + (221 - legendWidth) / 2
        gfx.drawString(legendName, legendNameX, 422 + font32Metrics.ascent)

        gfx.font = font18
        gfx.color = lightGray
        //gfx.drawString("Total Kills: ${profile.total.kills.value}", 13, 488)

        val entries = profile.legends.selected.data.filter { it.name != null }.take(6)
        for ((i, entry) in entries.withIndex()) {
            val offset = 425 + (28 * (i + 1) * 2)
            gfx.font = font20
            gfx.drawString("${entry.name}", 13, offset)
            gfx.font = font18
            gfx.drawString("-> ${entry.value}", 13, offset + 27)
        }

        val rank = profile.global.rank
        val riResource = Resources.rank("${rank.rankName.decapitalize()}${rank.rankDiv}")
        val rankIcon = ImageIO.read(riResource)

        gfx.drawImage(rankIcon, 300, 240, null)

        val rankHeight = when {
            rank.rankName == "Apex Predator" -> 260
            else -> 280
        }
        gfx.color = notQuiteWhite
        val subTextMetrics = gfx.fontMetrics
        gfx.drawString("Platform: ${profile.global.platform}", 280, 365 + subTextMetrics.ascent)
        gfx.drawString("${rank.rankName} (Division ${rank.rankDiv})", rankHeight, 382 + font32Metrics.ascent)

        gfx.drawString("Rank Points: ${rank.rankScore}", 280, 419 + subTextMetrics.ascent)

        gfx.color = white
        gfx.fillRect(275, 455, 174, 5)

        val rpb = RankPointBorder[rank.rankName]!!.toDouble()
        val rdp = RankDivisionPoint[rank.rankName]!!.toDouble()
        val rpdiv = ((rank.rankScore - rpb) / rdp)
        val rankx = (174 * (rpdiv - rpdiv.toInt())).toInt()
        gfx.color = blue
        gfx.fillRect(275, 455, rankx, 5)

        var rankDiff = userInfo.beforeRankPoint - rank.rankScore
        val upDown: String
        when {
            rankDiff < 0 -> {
                rankDiff *= -1
                gfx.color = lightBlue
                upDown = "$rankDiff up"
            }
            rankDiff > 0 -> {
                gfx.color = barRed
                upDown = "$rankDiff down"
            }
            else -> {
                gfx.color = white
                upDown = ""
            }
        }
        if (upDown.isNotEmpty()){
            gfx.drawString("Your RP is $upDown", 285, 467 + subTextMetrics.ascent)
            gfx.color = white

            gfx.drawString("from previous view", 285, 487 + subTextMetrics.ascent)
        }

        gfx.dispose()

        return ByteArrayOutputStream().use {
            ImageIO.write(base, "png", it)
            it.toByteArray()
        }
    }

}
