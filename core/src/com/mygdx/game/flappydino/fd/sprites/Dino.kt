package com.mygdx.flappydino.fd.sprites

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3

class Dino(x: Int, y: Int) {
    companion object {
        private const val GRAVITY = -15
        private const val MOVEMENT = 100
    }

    val position = Vector3(x.toFloat(), y.toFloat(), 0f)
    private val velocity = Vector3(0f, 0f, 0f)
    private var texture = Texture("dinoanimation.png")
    private val dinoAnimation = Animation(TextureRegion(texture), 4, 0.5f)
    private var dieTexture = Texture("dinodie.png")
    private val dieAnimation = Animation(TextureRegion(dieTexture), 3, 0.5f)
    private val bounds = Rectangle(x.toFloat(), y.toFloat(), (texture.width / 4).toFloat(), texture.height.toFloat())
    var colliding = false

    fun update(dt: Float) {
        if (colliding) {
            dieAnimation.update(dt)
        } else {
            dinoAnimation.update(dt)
            velocity.add(0f, GRAVITY.toFloat(), 0f)
            velocity.scl(dt)
            if (!colliding) position.add(MOVEMENT * dt, velocity.y, 0f)
            if (position.y < 82) position.y = 82f

            velocity.scl(1 / dt)
            updateBounds()
        }
    }

    fun jump() {
        if (!colliding) {
            velocity.y = 250f
        }
    }

    private fun updateBounds() {
        bounds.setPosition(position.x, position.y)
    }

    fun getX(): Float {
        return position.x
    }

    fun getY(): Float {
        return position.y
    }

    fun getWidth(): Float {
        return texture.width.toFloat()
    }

    fun getHeight(): Float {
        return texture.height.toFloat()
    }

    fun getTexture(): TextureRegion {
        return if (colliding) {
            dieAnimation.getFrame()
        } else {
            dinoAnimation.getFrame()
        }
    }

    fun getBounds(): Rectangle {
        return bounds
    }

    fun dispose() {
        texture.dispose()
        dieTexture.dispose()
    }

    fun setTexture(texturePath: String) {
        texture.dispose() // 기존 텍스처 해제
        texture = Texture(texturePath)
    }
}
