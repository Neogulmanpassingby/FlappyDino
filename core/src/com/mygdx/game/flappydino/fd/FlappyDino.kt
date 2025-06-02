package com.mygdx.flappydino.fd

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.mygdx.flappydino.fd.states.GameStateManager
import com.mygdx.flappydino.fd.states.MenuState

class FlappyDino(private val actionResolver: ActionResolver, private val username: String) : ApplicationAdapter() {

    interface ActionResolver {
        fun showMainActivity()
        fun showLeaderboardActivity()
        fun updateLeaderboard(username: String, score: Int)
    }

    companion object {
        const val WIDTH = 480
        const val HEIGHT = 800
        const val SCALE = 0.5f
        const val TITLE = "FlappyDino"
    }

    private lateinit var spriteBatch: SpriteBatch
    private lateinit var gameStateManager: GameStateManager

    override fun create() {
        spriteBatch = SpriteBatch()
        gameStateManager = GameStateManager()
        gameStateManager.push(MenuState(gameStateManager,username))

        Gdx.gl.glClearColor(1f, 0f, 0f, 1f)
    }

    override fun render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        gameStateManager.update(Gdx.graphics.deltaTime)
        gameStateManager.render(spriteBatch)
    }

    fun getActionResolver(): ActionResolver {
        return actionResolver
    }

    fun getUsername(): String {
        return username
    }
}
