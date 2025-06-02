package com.mygdx.game

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.gson.Gson
import com.mygdx.game.flappydino.R
import java.security.MessageDigest

class MainActivity : AppCompatActivity() {
    private lateinit var callbackManager: CallbackManager
    private var shouldLogoutOnResume = false
    private lateinit var soundPool: SoundPool
    private var soundId: Int = 0
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Facebook SDK 초기화 전 설정
        FacebookSdk.setAdvertiserIDCollectionEnabled(true)
        FacebookSdk.setAutoLogAppEventsEnabled(true)
        FacebookSdk.setApplicationId(getString(R.string.facebook_app_id))
        FacebookSdk.sdkInitialize(applicationContext)

        setContentView(R.layout.activity_main)

        // SoundPool 초기화
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        // 사운드 로드
        soundId = soundPool.load(this, R.raw.click_sound, 1)

        // MediaPlayer 초기화 및 배경음악 재생
        mediaPlayer = MediaPlayer.create(this, R.raw.backgroundmusic1)
        mediaPlayer.isLooping = true // 반복 재생
        mediaPlayer.start()

        // KeyHash 코드
        try {
            val info: PackageInfo = packageManager.getPackageInfo(
                "com.mygdx.game",
                PackageManager.GET_SIGNATURES
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                for (sig in info.signingInfo.apkContentsSigners) {
                    val md: MessageDigest = MessageDigest.getInstance("SHA")
                    md.update(sig.toByteArray())
                    Log.d("KeyHash", Base64.encodeToString(md.digest(), Base64.DEFAULT))
                }
            } else {
                for (sig in info.signatures) {
                    val md: MessageDigest = MessageDigest.getInstance("SHA")
                    md.update(sig.toByteArray())
                    Log.d("KeyHash", Base64.encodeToString(md.digest(), Base64.DEFAULT))
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        callbackManager = CallbackManager.Factory.create()

        val loginBtn = findViewById<LoginButton>(R.id.login_button)
        val mainLayout = findViewById<RelativeLayout>(R.id.mainLayout)
        val imageView = findViewById<ImageView>(R.id.MovingDino)
        val spriteAnimation = createSpriteAnimation()
        imageView.setImageDrawable(spriteAnimation)
        spriteAnimation.start()

        loginBtn.setPermissions(*arrayOf("public_profile", "email"))
        loginBtn.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onCancel() {
                Toast.makeText(this@MainActivity, "Facebook Login Cancelled", Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: FacebookException) {
                Toast.makeText(this@MainActivity, "Facebook Login Error Occurred...", Toast.LENGTH_SHORT).show()
                Log.d("onError", "error : ${error.toString()}")
            }

            override fun onSuccess(result: LoginResult) {
                val graphRequest = GraphRequest.newMeRequest(result.accessToken) { f_object, response ->
                    runOnUiThread {
                        if (f_object != null) {
                            val gson = Gson()
                            val user = gson.fromJson(f_object.toString(), DataModel::class.java)
                            Log.d("onSuccess", "onSuccess: token: ${result.accessToken.token} \n\n userObject: $f_object \n\n $response")

                            // AndroidLauncher로 Intent로 데이터 전달
                            shouldLogoutOnResume = true
                            val intent = Intent(this@MainActivity, AndroidLauncher::class.java)
                            intent.putExtra("user", user)
                            intent.putExtra("shouldLogoutOnResume", true)
                            startActivity(intent)
                            soundPool.release()
                            mediaPlayer.release()
                        } else {
                            Log.d("onSuccess", "Facebook object is null")
                        }
                    }
                }
                val parameters = Bundle()
                parameters.putString("fields", "id,name,email,birthday")
                graphRequest.parameters = parameters
                graphRequest.executeAsync()
            }
        })

        mainLayout.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val touchX = event.x
                val touchY = event.y

                // 클릭 시 소리 재생
                soundPool.play(soundId, 1f, 1f, 0, 0, 1f)

                animateImages(touchX, touchY, v)
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        // 페이스북 로그아웃
        shouldLogoutOnResume = intent.getBooleanExtra("shouldLogoutOnResume", false)
        if (shouldLogoutOnResume && AccessToken.getCurrentAccessToken() != null) {
            shouldLogoutOnResume = false
            LoginManager.getInstance().logOut()
            Toast.makeText(this, "Logged out from Facebook", Toast.LENGTH_SHORT).show()
        }
    }

    private fun animateImages(touchX: Float, touchY: Float, view: View) {
        val parentView = view as RelativeLayout

        for (i in 0 until 8) {
            val imageView = ImageView(this).apply {
                setImageResource(R.drawable.hatch)
                layoutParams = RelativeLayout.LayoutParams(100, 100).apply {
                    leftMargin = touchX.toInt() - 50
                    topMargin = touchY.toInt() - 50
                }
                alpha = 1f  // 초기 투명도 설정
            }
            parentView.addView(imageView)

            val endX = touchX + (Math.random() * 200 - 100).toFloat() // Random X within ±100 pixels
            val endY = touchY + (Math.random() * 200 - 100).toFloat() // Random Y within ±100 pixels
            val scaleX = ObjectAnimator.ofFloat(imageView, "scaleX", 0f, 1f).apply { duration = 500 }
            val scaleY = ObjectAnimator.ofFloat(imageView, "scaleY", 0f, 1f).apply { duration = 500 }
            val moveX = ObjectAnimator.ofFloat(imageView, "x", touchX - 50, endX).apply { duration = 500 }
            val moveY = ObjectAnimator.ofFloat(imageView, "y", touchY - 50, endY).apply { duration = 500 }
            val fadeOut = ObjectAnimator.ofFloat(imageView, "alpha", 1f, 0f).apply { duration = 500 }

            AnimatorSet().apply {
                playTogether(scaleX, scaleY, moveX, moveY, fadeOut)
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        parentView.removeView(imageView)
                    }
                })
                start()
            }
        }
    }

    private fun createSpriteAnimation(): AnimationDrawable {
        val spriteAnimation = AnimationDrawable()
        spriteAnimation.isOneShot = false  // 반복 재생 설정

        val spriteSheet = BitmapFactory.decodeResource(resources, R.drawable.move)
        val frameWidth = spriteSheet.width / 6  // 각 프레임의 너비
        val frameHeight = spriteSheet.height  // 각 프레임의 높이

        for (i in 0 until 6) {
            val frame = Bitmap.createBitmap(spriteSheet, i * frameWidth, 0, frameWidth, frameHeight)
            val drawable = BitmapDrawable(resources, frame)
            spriteAnimation.addFrame(drawable, 100)  // 각 프레임의 지속 시간(100ms)
        }

        return spriteAnimation
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }
}
