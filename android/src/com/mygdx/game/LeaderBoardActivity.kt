package com.mygdx.game

import LeaderboardAdapter
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.mygdx.game.flappydino.R
import kotlinx.coroutines.*
import java.io.InputStream

class LeaderBoardActivity : AppCompatActivity() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var leaderboardListView: ListView
    private lateinit var leaderboardTitle: TextView
    private lateinit var adapter: LeaderboardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leader_board)

        // Initialize MediaPlayer
        mediaPlayer = MediaPlayer.create(this, R.raw.backgroundmusic3)

        leaderboardListView = findViewById(R.id.leaderboardListView)
        leaderboardTitle = findViewById(R.id.leaderboardTitle)

        // 커스텀 폰트 설정
        val customFont: Typeface = Typeface.createFromAsset(assets, "myfont.ttf")
        leaderboardTitle.typeface = customFont

        coroutineScope.launch {
            val result = loadLeaderboardData()
            withContext(Dispatchers.Main) {
                adapter = LeaderboardAdapter(this@LeaderBoardActivity, result)
                leaderboardListView.adapter = adapter
                playSound() // Play the sound when the leaderboard is updated
            }
        }
    }

    private suspend fun loadLeaderboardData(): List<Pair<String, Int>> = withContext(Dispatchers.IO) {
        try {
            val credentialsStream: InputStream = resources.openRawResource(R.raw.credentials)
            val credentials = GoogleCredentials.fromStream(credentialsStream)
                .createScoped(listOf(SheetsScopes.SPREADSHEETS))
            val sheetsService = Sheets.Builder(
                com.google.api.client.http.javanet.NetHttpTransport(),
                com.google.api.client.json.gson.GsonFactory(),
                HttpCredentialsAdapter(credentials)
            )
                .setApplicationName("FlappyDino")
                .build()

            val range = "Leaderboard!A:B"
            val result = sheetsService.spreadsheets().values()
                .get("1OKmjtE7R_y_grwfGDysWfm5njD2Tj3lSrBPOUIDprRY", range)
                .execute()
            val values = result.getValues() ?: emptyList<List<Any>>()

            // 데이터 정렬
            val leaderboardData = values.mapNotNull { row ->
                val username = row.getOrNull(0)?.toString()
                val score = row.getOrNull(1)?.toString()?.toIntOrNull()
                if (username != null && score != null) {
                    username to score
                } else {
                    null
                }
            }.sortedByDescending { it.second }

            return@withContext leaderboardData
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    private fun playSound() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }
}
