package com.mygdx.flappydino.fd.sprites

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import java.util.*

class Tube(x: Float) {
    companion object {
        const val TUBE_WIDTH = 52
        private const val TUBE_GAP = 100
        private const val LOWEST_OPENING = 120
        private const val FLUCTUATION = 130
    }

    private val topTube = Texture("toptube.png")
    private val bottomTube = Texture("bottomtube.png")
    private val rand = Random()
    private val posTopTube = Vector2(x, (rand.nextInt(FLUCTUATION) + LOWEST_OPENING + TUBE_GAP).toFloat())
    private val posBottomTube = Vector2(x, posTopTube.y - TUBE_GAP - bottomTube.height)
    private val boundsTop = Rectangle(posTopTube.x, posTopTube.y, topTube.width.toFloat(), topTube.height.toFloat())
    private val boundsBottom = Rectangle(posBottomTube.x, posBottomTube.y, bottomTube.width.toFloat(), bottomTube.height.toFloat())
    var passed = false  // 파이프를 통과했는지 여부를 확인하는 변수

    fun reposition(x: Float) {
        posTopTube.set(x, (rand.nextInt(FLUCTUATION) + LOWEST_OPENING + TUBE_GAP).toFloat())
        posBottomTube.set(x, posTopTube.y - TUBE_GAP - bottomTube.height)
        boundsTop.setPosition(posTopTube.x, posTopTube.y)
        boundsBottom.setPosition(posBottomTube.x, posBottomTube.y)
        passed = false  // 파이프 위치를 재설정할 때 passed 변수 초기화
    }

    fun collides(player: Rectangle): Boolean {
        return player.overlaps(boundsBottom) || player.overlaps(boundsTop)
    }

    fun getTopTube(): Texture {
        return topTube
    }

    fun getBottomTube(): Texture {
        return bottomTube
    }

    fun getPosTopTube(): Vector2 {
        return posTopTube
    }

    fun getPosBottomTube(): Vector2 {
        return posBottomTube
    }

    fun getBoundsTop(): Rectangle {
        return boundsTop
    }

    fun getBoundsBottom(): Rectangle {
        return boundsBottom
    }
}
