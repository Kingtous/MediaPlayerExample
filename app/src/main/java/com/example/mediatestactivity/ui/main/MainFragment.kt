package com.example.mediatestactivity.ui.main

import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Toast
import com.example.mediatestactivity.R
import kotlinx.android.synthetic.main.main_fragment.*
import java.lang.IllegalStateException
import java.util.concurrent.*

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
        var TAG = "media_test"
    }

    private lateinit var viewModel: MainViewModel
    private var mediaPlayer: MediaPlayer? = null
    /// 按下了stop
    private var isStopped = false

    private var mExecutor: ScheduledExecutorService?=null
    private var updateProgressCallback: Runnable?=null

    /// 表明视频已经开始播放，用于控制生命周期
    private var started = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onDestroy() {
        mediaPlayer?.let {
            it.release()
        }
        mediaPlayer = null
        super.onDestroy()
    }

    override fun onPause() {
        mediaPlayer?.let {
            if (it.isPlaying){
                it.pause()
            }
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer?.let {
            if (started && !it.isPlaying){
                try {
                    it.start()
                } catch (e:IllegalStateException){

                }
            }
        }
    }

    fun getTimeStr(time:Int):String{
        return (time/60).toString().format("%0d")+":"+(time%60).toString().format("%0d")
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        val mediaUri = Uri.parse(viewModel.mv)
        // 初始化媒体播放器
        mediaPlayer = MediaPlayer.create(context, mediaUri)
        mediaPlayer?.setOnCompletionListener {
            btn_stop.performClick()
        }
        // prepare Surface
        mediaSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                Log.d(TAG,"surfaceChanged")

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Log.d(TAG,"surfaceDestroyed")
//                mediaPlayer?.let {
//                    it.stop()
//                }
            }

            override fun surfaceCreated(holder: SurfaceHolder) {
                mediaPlayer?.let { player ->
                    Log.d(TAG,"surfaceCreated")
                    player.setDisplay(holder)
                }
            }
        })
        btn_start.setOnClickListener {
            mediaPlayer?.let {
                if (it.isPlaying){
                    return@setOnClickListener
                }
            }
            try {
                Log.d(TAG, "start playing");
                if (isStopped) {
                    Log.d(TAG, "start prepare")
                    try {
                        mediaPlayer?.prepare()
                        isStopped = false
                    } catch (e: IllegalStateException){

                    }
                }
                mediaPlayer?.start()
                activity?.let {
                    it.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                started = true
                mediaPlayer?.let {
                    if (mExecutor!=null){
                        mExecutor?.shutdownNow()
                    }
                    mExecutor = Executors.newSingleThreadScheduledExecutor()
                    if (updateProgressCallback == null){
                        updateProgressCallback = Runnable {
                            Log.d(TAG,"update duration position")
                            mediaPlayer?.let {
                                if (it.isPlaying){
                                    Log.d(TAG,"${it.currentPosition}")
                                    activity?.let { act ->
                                        act.runOnUiThread {
                                            tv_currentTime.text = getTimeStr(it.currentPosition/1000)
                                            tv_totalTime.text = getTimeStr(it.duration/1000)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    mExecutor?.scheduleAtFixedRate(updateProgressCallback,0,1,TimeUnit.SECONDS)
                }
            } catch (e:IllegalStateException){
                context?.let {
                    Toast.makeText(it,e.message.toString(),Toast.LENGTH_SHORT)
                }

            }
        }
        btn_pause.setOnClickListener {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    activity?.let {
                        it.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }
            started = false
        }
        btn_stop.setOnClickListener {
            mediaPlayer?.stop()
            isStopped = true
            mExecutor?.shutdownNow()
            mExecutor = null
            updateProgressCallback = null
            started = false
            activity?.let {
                it.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

}