package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import android.widget.TextView
import android.os.Bundle
import com.example.myapplication.R
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.android.AndroidAudioPlayer
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchDetectionResult
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.pitch.PitchProcessor
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.writer.WriterProcessor
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.lang.Exception
import java.nio.ByteOrder

internal class RecordPlayActivity : AppCompatActivity() {
    var dispatcher: AudioDispatcher? = null
    var tarsosDSPAudioFormat: TarsosDSPAudioFormat? = null
    var file: File? = null
    var pitchTextView: TextView? = null
    var recordButton: Button? = null
    var playButton: Button? = null
    var isRecording = false
    var filename = "recorded_sound.wav"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val sdCard = Environment.getExternalStorageDirectory()
        file = File(sdCard, filename)

        /*
        filePath = file.getAbsolutePath();
        Log.e("MainActivity", "저장 파일 경로 :" + filePath); // 저장 파일 경로 : /storage/emulated/0/recorded.mp4
        */tarsosDSPAudioFormat = TarsosDSPAudioFormat(
            TarsosDSPAudioFormat.Encoding.PCM_SIGNED,
            22050f,
            2 * 8,
            1,
            2 * 1,
            22050f, ByteOrder.BIG_ENDIAN == ByteOrder.nativeOrder()
        )
        pitchTextView = findViewById(R.id.pitchTextView)
        recordButton = findViewById(R.id.recordButton)
        playButton = findViewById(R.id.playButton)
        recordButton!!.setOnClickListener(View.OnClickListener {
            if (!isRecording) {
                Log.d("recordButton","누름")
                recordAudio()
                isRecording = true
                recordButton!!.setText("중지")
            } else {
                stopRecording()
                isRecording = false
                recordButton!!.setText("녹음")
            }
        })
        playButton!!.setOnClickListener(View.OnClickListener { playAudio() })
    }

    fun playAudio() {
        try {
            releaseDispatcher()
            val fileInputStream = FileInputStream(file)
            dispatcher = AudioDispatcher(
                UniversalAudioInputStream(fileInputStream, tarsosDSPAudioFormat),
                1024,
                0
            )
            val playerProcessor: AudioProcessor = AndroidAudioPlayer(tarsosDSPAudioFormat, 2048, 0)
            dispatcher!!.addAudioProcessor(playerProcessor)
            val pitchDetectionHandler = PitchDetectionHandler { res, e ->
                val pitchInHz = res.pitch
                runOnUiThread { pitchTextView!!.text = pitchInHz.toString() + "" }
            }
            val pitchProcessor: AudioProcessor = PitchProcessor(
                PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
                22050f,
                1024,
                pitchDetectionHandler
            )
            dispatcher!!.addAudioProcessor(pitchProcessor)
            val audioThread = Thread(dispatcher, "Audio Thread")
            audioThread.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun recordAudio() {
        releaseDispatcher()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO),
                123
            )
            Log.d("녹음버튼","if")
        }
        else{
            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0)
            Log.d("녹음버튼","else")
        }
        try {
            val randomAccessFile = RandomAccessFile(file, "rw")
            val recordProcessor: AudioProcessor =
                WriterProcessor(tarsosDSPAudioFormat, randomAccessFile)
            dispatcher!!.addAudioProcessor(recordProcessor)
            val pitchDetectionHandler = PitchDetectionHandler { res, e ->
                val pitchInHz = res.pitch
                runOnUiThread { pitchTextView!!.text = pitchInHz.toString() + "" }
            }
            val pitchProcessor: AudioProcessor = PitchProcessor(
                PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
                22050f,
                1024,
                pitchDetectionHandler
            )
            dispatcher!!.addAudioProcessor(pitchProcessor)
            val audioThread = Thread(dispatcher, "Audio Thread")
            audioThread.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    fun checkRecordPermission() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO),
                123
            )
        }
    }
    fun stopRecording() {
        releaseDispatcher()
    }

    fun releaseDispatcher() {
        if (dispatcher != null) {
            if (!dispatcher!!.isStopped) dispatcher!!.stop()
            dispatcher = null
        }
    }

    override fun onStop() {
        super.onStop()
        releaseDispatcher()
    }
}