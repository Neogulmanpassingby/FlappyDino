package com.mygdx.flappydino.fd.sprites

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array

class Animation(region: TextureRegion, private val frameCount: Int, cycleTime: Float) {
    private val frames: Array<TextureRegion> = Array()
    private var maxFrameTime: Float = cycleTime / frameCount
    private var currentFrameTime: Float = 0f
    private var frame: Int = 0

    init {
        val frameWidth = region.regionWidth / frameCount
        for (i in 0 until frameCount) {
            frames.add(TextureRegion(region, i * frameWidth, 0, frameWidth, region.regionHeight))
        }
    }

    fun update(dt: Float) {
        currentFrameTime += dt
        if (currentFrameTime > maxFrameTime) {
            frame++
            currentFrameTime = 0f
        }
        if (frame >= frameCount) frame = 0
    }

    fun flip() {
        for (region in frames) region.flip(true, false)
    }

    fun getFrame(): TextureRegion {
        return frames[frame]
    }
}