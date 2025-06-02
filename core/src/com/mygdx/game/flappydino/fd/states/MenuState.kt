package com.mygdx.flappydino.fd.states

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.mygdx.flappydino.fd.FlappyDino

class MenuState(gsm: GameStateManager, private val username: String) : State(gsm) {
    private val background = Texture("bg.png")
    private val playBtn = Texture("playbtn.png")

    init {
        cam.setToOrtho(false, FlappyDino.WIDTH.toFloat(), FlappyDino.HEIGHT.toFloat())
    }

    override fun handleInput() {
        if (Gdx.input.justTouched()) {
            gsm.set(PlayState(gsm, username))
        }
    }

    override fun update(dt: Float) {
        handleInput()
    }

    override fun render(sb: SpriteBatch) {
        sb.projectionMatrix = cam.combined
        sb.begin()
        sb.draw(background, 0f, 0f, FlappyDino.WIDTH.toFloat(), FlappyDino.HEIGHT.toFloat())
        sb.draw(playBtn, (cam.viewportWidth / 2 - playBtn.width / 2), (cam.viewportHeight / 2 - playBtn.height / 2))
        sb.end()
    }

    override fun dispose() {
        background.dispose()
        playBtn.dispose()
    }
}
