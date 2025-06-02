package com.mygdx.flappydino.fd.states

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3

abstract class State(protected val gsm: GameStateManager) {
    protected val cam = OrthographicCamera()
    protected val mouse = Vector3()

    init {
        cam.setToOrtho(false, 240f, 400f)
    }

    abstract fun handleInput()
    abstract fun update(dt: Float)
    abstract fun render(sb: SpriteBatch)
    open fun dispose() {}  // dispose 메소드 추가
}
