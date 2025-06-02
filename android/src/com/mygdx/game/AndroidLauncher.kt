package com.mygdx.game

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.mygdx.flappydino.fd.FlappyDino
import com.mygdx.game.flappydino.R
import kotlinx.coroutines.*
import java.io.InputStream
import kotlin.math.pow

class AndroidLauncher : AndroidApplication(), FlappyDino.ActionResolver {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var mediaPlayer: MediaPlayer // MediaPlayer 변수 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Intent로 데이터 받아오기
        val user: DataModel? = intent.getParcelableExtra("user")
        if (user != null) {
            Log.d("AndroidLauncher", "User data received: ${user.name}")
        } else {
            Log.d("AndroidLauncher", "No user data received.")
        }

        val config = AndroidApplicationConfiguration()
        initialize(FlappyDino(this, user?.name ?: "Unknown"), config)

        // MediaPlayer 초기화 및 배경 음악 재생 시작
        mediaPlayer = MediaPlayer.create(this, R.raw.backgroundmusic2)
        mediaPlayer.isLooping = true // 반복 재생 설정
        mediaPlayer.setVolume(0.5f, 0.5f) // 볼륨 설정 (좌, 우)
        mediaPlayer.start() // 재생 시작
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release() // MediaPlayer 해제
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer.pause() // MediaPlayer 일시정지
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer.start() // MediaPlayer 재생
    }

    override fun showMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("shouldLogoutOnResume", true)
        startActivity(intent)
        finish() // AndroidLauncher 액티비티 종료
    }

    override fun showLeaderboardActivity() {
        val intent = Intent(this, LeaderBoardActivity::class.java)
        startActivity(intent)
    }

    override fun updateLeaderboard(username: String, score: Int) {
        coroutineScope.launch {
            try {
                updateLeaderboardDataWithRetry(username, score)
            } catch (e: Exception) {
                Log.e("AndroidLauncher", "Error updating leaderboard", e)
            }
        }
    }

    private suspend fun updateLeaderboardData(username: String, score: Int) {
        try {
            val credentialsStream: InputStream = resources.openRawResource(R.raw.credentials)
            val credentials = GoogleCredentials.fromStream(credentialsStream)
                .createScoped(listOf(SheetsScopes.SPREADSHEETS))
            val sheetsService = Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                HttpCredentialsAdapter(credentials)
            )
                .setApplicationName("FlappyDino")
                .build()

            // A열과 B열의 모든 값을 가져옴
            val range = "Leaderboard!A:B"
            val result = sheetsService.spreadsheets().values()
                .get("1OKmjtE7R_y_grwfGDysWfm5njD2Tj3lSrBPOUIDprRY", range)
                .execute()
            val values = result.getValues() ?: emptyList<List<Any>>()

            // 이름이 이미 존재하는지 확인하고, 기존 점수와 비교
            var updateNeeded = true
            var rowIndexToUpdate = -1
            for ((index, row) in values.withIndex()) {
                if (row.isNotEmpty() && row[0] == username) {
                    val existingScore = row.getOrNull(1)?.toString()?.toIntOrNull()
                    if (existingScore != null && existingScore >= score) {
                        updateNeeded = false
                    } else {
                        rowIndexToUpdate = index
                    }
                    break
                }
            }

            if (!updateNeeded) {
                Log.d("AndroidLauncher", "Username $username already has a higher or equal score. Skipping update.")
                return
            }

            val valuesToUpdate = listOf(
                listOf(username, score)
            )
            val body = ValueRange().setValues(valuesToUpdate)

            if (rowIndexToUpdate == -1) {
                // 새로운 항목 추가
                sheetsService.spreadsheets().values()
                    .append("1OKmjtE7R_y_grwfGDysWfm5njD2Tj3lSrBPOUIDprRY", "Leaderboard", body)
                    .setValueInputOption("RAW")
                    .execute()
            } else {
                // 기존 항목 업데이트
                val updateRange = "Leaderboard!A${rowIndexToUpdate + 1}:B${rowIndexToUpdate + 1}"
                sheetsService.spreadsheets().values()
                    .update("1OKmjtE7R_y_grwfGDysWfm5njD2Tj3lSrBPOUIDprRY", updateRange, body)
                    .setValueInputOption("RAW")
                    .execute()
            }

        } catch (e: Exception) {
            throw e
        }
    }

    private suspend fun updateLeaderboardDataWithRetry(username: String, score: Int) {
        val maxAttempts = 5
        var attempt = 0
        var delayMillis = 1000L // Initial delay is 1 second

        while (attempt < maxAttempts) {
            try {
                updateLeaderboardData(username, score)
                return // 성공하면 함수 종료
            } catch (e: Exception) {
                attempt++
                if (attempt >= maxAttempts) {
                    throw e // 최대 시도 횟수를 초과하면 예외를 던짐
                }
                Log.w("AndroidLauncher", "Retry $attempt/$maxAttempts due to ${e.message}")
                delay(delayMillis)
                delayMillis = (delayMillis * 2.0.pow(attempt)).toLong() // 지수 백오프
            }
        }
    }
}
