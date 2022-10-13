package com.example.LoopStationSpeaker


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.LoopStationSpeaker.R
import kotlinx.android.synthetic.main.activity_main.*
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.io.File
import java.io.FileInputStream
import java.io.IOException


private const val LOG_TAG = "AudioRecordTest"
private const val REQUEST_RECORD_AUDIO_PERMISSION = 200


class MainActivity : AppCompatActivity() {
    private lateinit var mqttAndroidClient: MqttAndroidClient
    private var fileName: String = ""
    private var player: MediaPlayer? = null
    private var recorder: MediaRecorder? = null
    var poisition = 0
    private var permissionToRecordAccepted = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)
    private var state:Boolean=false

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (!permissionToRecordAccepted) finish()
    }


    private fun onPlay(start: Boolean) = if (start) {
        startPlaying()
    } else {
        stopPlaying()
    }

    private fun startPlaying() {
        player = MediaPlayer().apply {
            try {
                setOnCompletionListener{
                    speakerImage.setImageResource(R.drawable.speakeroff)
                }
                var fs = FileInputStream(fileName)
                var fd = fs.fd
                setDataSource(fd)
                prepare()
                start()
                //Toast.makeText(applicationContext,fileName,Toast.LENGTH_LONG).show()
            } catch (e: IOException) {
                Toast.makeText(applicationContext,e.message,Toast.LENGTH_LONG).show()
                Log.e(LOG_TAG, "prepare() failed")
                speakerImage.setImageResource(R.drawable.speakeroff)
            }
        }
    }

    private fun stopPlaying() {
        player?.release()
        player = null
    }

    private fun startRecording() {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }

            start()
        }

    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()

        }
        recorder = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)
        fileName = "${externalCacheDir?.absolutePath}/audiorecordtest.3gpp"


        btn.setOnClickListener {
            this.connect(this.applicationContext)
        }
        subtn.setOnClickListener {
            subscribe("CTRL-SPEAKER")
            receiveMessages()

        }

    }
    override fun onStop() {
        super.onStop()
        player?.release()
        player = null

    }
    private fun connect(applicationContext : Context) {
        try {
            mqttAndroidClient = MqttAndroidClient(applicationContext,"tcp://"+ev.text.toString()+":1883",MqttClient.generateClientId())
            var a = mqttAndroidClient.connect()
            //Toast.makeText(applicationContext,"tcp://"+ev.text.toString()+":1883", Toast.LENGTH_SHORT).show()
                a.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken){
                    Log.i("Connection", "success ")
                    Toast.makeText(applicationContext,"Connection",Toast.LENGTH_LONG).show()
                    //connectionStatus = true
                    // Give your callback on connection established here
                }
                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    //connectionStatus = false
                    Log.i("Connection", "failure")
                    // Give your callback on connection failure here
                    Toast.makeText(applicationContext,"connect fail",Toast.LENGTH_SHORT).show()
                    exception.printStackTrace()
                }
            }
        } catch (e: MqttException) {
            // Give your callback on connection failure here
            //e.printStackTrace()
            Toast.makeText(applicationContext,"Exception",Toast.LENGTH_SHORT).show()
        }

    }
    private fun subscribe(topic: String) {
        val qos = 2 // Mention your qos value
        try {
            mqttAndroidClient.subscribe(topic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Toast.makeText(applicationContext,"subscribed",Toast.LENGTH_SHORT).show()
                }
                override fun onFailure(
                    asyncActionToken: IMqttToken,
                    exception: Throwable
                ) {
                    Toast.makeText(applicationContext,"fail",Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: MqttException) {

        }
    }
    fun unSubscribe(topic: String) {
        try {
            val unsubToken = mqttAndroidClient.unsubscribe(topic)
            unsubToken.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    // Give your callback on unsubscribing here
                }
                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    // Give your callback on failure here
                }
            }
        } catch (e: MqttException) {
            // Give your callback on failure here
        }
    }
    fun receiveMessages() {
        mqttAndroidClient.setCallback(object : MqttCallback {

            override fun connectionLost(cause: Throwable) {

            }
            override fun messageArrived(topic: String, message: MqttMessage) {
                try {
                    speakerImage.setImageResource(R.drawable.speakeroff)
                    onPlay(false)

                    var buf = File(fileName).outputStream()
                    val payload1 = message.payload
                    buf.write(payload1)
                    speakerImage.setImageResource(R.drawable.speakeron)
                    onPlay(true)

                } catch (e: Exception) {
                    // Give your callback on error here

                    Toast.makeText(applicationContext,e.message,Toast.LENGTH_LONG).show()
                }
            }
            override fun deliveryComplete(token: IMqttDeliveryToken) {
                // Acknowledgement on delivery complete
            }
        })
    }

    fun disconnect() {
        try {
            val disconToken = mqttAndroidClient.disconnect()
            disconToken.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    //connectionStatus = false
                    // Give Callback on disconnection here
                }
                override fun onFailure(
                    asyncActionToken: IMqttToken,
                    exception: Throwable
                ) {
                    // Give Callback on error here
                }
            }
        } catch (e: MqttException) {
            // Give Callback on error here
        }
    }
}
