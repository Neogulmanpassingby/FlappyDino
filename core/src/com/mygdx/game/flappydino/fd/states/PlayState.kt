package com.mygdx.flappydino.fd.states

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.mygdx.flappydino.fd.FlappyDino
import com.mygdx.flappydino.fd.sprites.Dino
import com.mygdx.flappydino.fd.sprites.Tube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlayState(gsm: GameStateManager, private val username: String) : State(gsm) {
    companion object {
        private const val GROUND_Y_OFFSET = -30
        private const val TUBE_SPACING = 125
        private const val TUBE_COUNT = 4
        private const val BUTTON_SCALE = 0.5f // 버튼 크기 조절을 위한 스케일
    }

    private val dino = Dino(40, 200)
    private val background = Texture("bg.png")
    private val ground = Texture("ground.png")
    private val gameoverImg = Texture("gameover.png")
    private val restart = Texture("restart.png")
    private val home = Texture("home.png")
    private val leaderboard = Texture("leaderboard.png")
    private val groundPos1 = Vector2(cam.position.x - cam.viewportWidth / 2, GROUND_Y_OFFSET.toFloat())
    private val groundPos2 = Vector2((cam.position.x - cam.viewportWidth / 2) + ground.width, GROUND_Y_OFFSET.toFloat())
    private val tubes = Array<Tube>()
    private val sr = ShapeRenderer()
    private var gameover = false
    private var score = 0
    private val font: BitmapFont
    private var leaderboardUpdated = false // 추가: 리더보드가 업데이트되었는지 여부를 추적

    // 효과음 추가
    private val jumpSound: Sound = Gdx.audio.newSound(Gdx.files.internal("jump.mp3"))
    private val deathSound: Sound = Gdx.audio.newSound(Gdx.files.internal("death.mp3"))
    private var deathSoundPlayed = false // 추가: 죽는 소리가 재생되었는지 여부를 추적

    // 버튼 영역 정의
    private val restartRect = Rectangle()
    private val homeRect = Rectangle()
    private val leaderboardRect = Rectangle()

    init {
        for (i in 1..TUBE_COUNT) {
            tubes.add(Tube(i * (TUBE_SPACING + Tube.TUBE_WIDTH).toFloat()))
        }

        // 폰트 생성
        val generator = FreeTypeFontGenerator(Gdx.files.internal("myfont.ttf"))
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            size = 50 // 폰트 크기 설정
            color = Color.WHITE // 폰트 색상
            borderWidth = 3f // 테두리 두께
            borderColor = Color.valueOf("fca048") // 테두리 색상
        }
        font = generator.generateFont(parameter)
        generator.dispose() // FreeTypeFontGenerator 해제

        // 버튼 위치 초기화
        updateButtonPositions()
    }

    private fun updateButtonPositions() {
        val gameoverY = cam.position.y + 20
        val restartX = cam.position.x - (restart.width * BUTTON_SCALE) / 2
        restartRect.set(
            restartX,
            gameoverY - (restart.height * BUTTON_SCALE) - 10,
            restart.width * BUTTON_SCALE,
            restart.height * BUTTON_SCALE
        )
        homeRect.set(
            restartX - (home.width * BUTTON_SCALE) - 10,
            restartRect.y,
            home.width * BUTTON_SCALE,
            home.height * BUTTON_SCALE
        )
        leaderboardRect.set(
            restartX + (restart.width * BUTTON_SCALE) + 10,
            restartRect.y,
            leaderboard.width * BUTTON_SCALE,
            leaderboard.height * BUTTON_SCALE
        )
    }

    override fun handleInput() {
        if (Gdx.input.isTouched) {
            val touchPos = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
            cam.unproject(touchPos)

            if (gameover) {
                if (restartRect.contains(touchPos.x, touchPos.y)) {
                    gsm.set(PlayState(gsm, username))
                } else if (homeRect.contains(touchPos.x, touchPos.y)) {
                    val flappyDino = Gdx.app.applicationListener as FlappyDino
                    flappyDino.getActionResolver().showMainActivity()
                } else if (leaderboardRect.contains(touchPos.x, touchPos.y)) {
                    val flappyDino = Gdx.app.applicationListener as FlappyDino
                    flappyDino.getActionResolver().showLeaderboardActivity()
                }
            } else {
                dino.jump()
                jumpSound.play()  // 점프할 때 효과음 재생
            }
        }
    }

    override fun update(dt: Float) {
        handleInput()
        updateGround()
        dino.update(dt)
        cam.position.set(dino.getX() + 80, cam.viewportHeight / 2, 0f)
        for (tube in tubes) {
            if (cam.position.x - cam.viewportWidth / 2 > tube.getPosTopTube().x + tube.getTopTube().width) {
                tube.reposition(tube.getPosTopTube().x + ((Tube.TUBE_WIDTH + TUBE_SPACING) * TUBE_COUNT))
            }

            if (tube.collides(dino.getBounds())) {
                dino.colliding = true
                gameover = true
                if (!deathSoundPlayed) { // 죽는 소리가 아직 재생되지 않았으면
                    deathSound.play()  // 죽었을 때 효과음 재생
                    deathSoundPlayed = true // 플래그 설정
                }
                if (!leaderboardUpdated) { // 리더보드가 아직 업데이트되지 않았으면
                    leaderboardUpdated = true // 플래그 설정
                    val flappyDino = Gdx.app.applicationListener as FlappyDino
                    CoroutineScope(Dispatchers.Main).launch {
                        flappyDino.getActionResolver().updateLeaderboard(username, score) // 여기서 Facebook 사용자의 이름을 전달해야 합니다.
                    }
                }
            }

            // Check if the dino has passed the tube
            if (!tube.passed && tube.getPosTopTube().x + tube.getTopTube().width < dino.getX()) {
                tube.passed = true
                score++
            }
        }
        if (dino.getY() <= ground.height + GROUND_Y_OFFSET) {
            gameover = true
            dino.colliding = true
            if (!deathSoundPlayed) { // 죽는 소리가 아직 재생되지 않았으면
                deathSound.play()  // 죽었을 때 효과음 재생
                deathSoundPlayed = true // 플래그 설정
            }
            if (!leaderboardUpdated) { // 리더보드가 아직 업데이트되지 않았으면
                leaderboardUpdated = true // 플래그 설정
                val flappyDino = Gdx.app.applicationListener as FlappyDino
                CoroutineScope(Dispatchers.Main).launch {
                    flappyDino.getActionResolver().updateLeaderboard(username, score) // 여기서 Facebook 사용자의 이름을 전달해야 합니다.
                }
            }
        }
        cam.update()
        updateButtonPositions()
    }

    private fun updateGround() {
        if (cam.position.x - cam.viewportWidth / 2 > groundPos1.x + ground.width)
            groundPos1.add(ground.width * 2f, 0f)
        if (cam.position.x - cam.viewportWidth / 2 > groundPos2.x + ground.width)
            groundPos2.add(ground.width * 2f, 0f)
    }

    override fun render(sb: SpriteBatch) {
        sb.projectionMatrix = cam.combined
        sb.begin()
        sb.draw(background, cam.position.x - cam.viewportWidth / 2, 0f)
        for (tube in tubes) {
            sb.draw(tube.getBottomTube(), tube.getPosBottomTube().x, tube.getPosBottomTube().y)
            sb.draw(tube.getTopTube(), tube.getPosTopTube().x, tube.getPosTopTube().y)
        }
        sb.draw(ground, groundPos1.x, groundPos1.y)
        sb.draw(ground, groundPos2.x, groundPos2.y)
        sb.draw(dino.getTexture(), dino.getX(), dino.getY())
        if (gameover) {
            sb.draw(gameoverImg, cam.position.x - gameoverImg.width / 2, cam.position.y + 20)
            sb.draw(restart, restartRect.x, restartRect.y, restartRect.width, restartRect.height)
            sb.draw(home, homeRect.x, homeRect.y, homeRect.width, homeRect.height)
            sb.draw(
                leaderboard,
                leaderboardRect.x,
                leaderboardRect.y,
                leaderboardRect.width,
                leaderboardRect.height
            )
        }

        // Draw score
        val scoreText = "$score"
        val layout = GlyphLayout(font, scoreText)
        val textWidth = layout.width
        font.draw(sb, scoreText, cam.position.x - textWidth / 2, cam.viewportHeight - 30)

        sb.end()
    }

    override fun dispose() {
        background.dispose()
        ground.dispose()
        gameoverImg.dispose()
        dino.dispose()
        for (tube in tubes) {
            tube.getTopTube().dispose()
            tube.getBottomTube().dispose()
        }
        font.dispose()
        jumpSound.dispose()  // 점프 소리 해제
        deathSound.dispose()  // 죽음 소리 해제
    }
}
