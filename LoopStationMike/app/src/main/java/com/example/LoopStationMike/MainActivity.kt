package com.example.LoopStationMike

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.io.File
import java.io.IOException
import java.nio.CharBuffer


private const val LOG_TAG = "AudioRecordTest"
private const val REQUEST_RECORD_AUDIO_PERMISSION = 200

class MainActivity : AppCompatActivity() {
    private lateinit var mqttAndroidClient: MqttAndroidClient
    private var fileName: String = ""
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    var poisition = 0
    private var permissionToRecordAccepted = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

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

    private fun onRecord(start: Boolean) = if (start) {
        startRecording()
    } else {
        stopRecording()
    }

    private fun onPlay(start: Boolean) = if (start) {
        startPlaying()
    } else {
        stopPlaying()
    }

    private fun startPlaying() {
        player = MediaPlayer().apply {
            try {
                setDataSource(fileName)
                prepare()
                start()

            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")

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

        btn.setOnClickListener {
            this.connect(this.applicationContext)
        }
        subtn.setOnClickListener {
            subscribe("CTRL-MIKE")
            receiveMessages()
        }
        fileName = "${externalCacheDir?.absolutePath}/audiorecordtest.3gp"

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)

    }
    override fun onStop() {
        super.onStop()
        recorder?.release()
        recorder = null
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
                    // Give your callback on Subscription here
                    Toast.makeText(applicationContext,"subscribed",Toast.LENGTH_SHORT).show()
                }
                override fun onFailure(
                    asyncActionToken: IMqttToken,
                    exception: Throwable
                ) {
                    // Give your subscription failure callback here
                    Toast.makeText(applicationContext,"fail",Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: MqttException) {
            // Give your subscription failure callback here
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
            override fun connectionLost(cause: Throwable){
            }
            override fun messageArrived(topic: String, message: MqttMessage) {
                try {
                    val data = String(message.payload, charset("UTF-8"))

                    var text = message.toString()
                    if(text.equals("MIKE ON"))
                    {
                        //Play 이미지
                        micImage.setImageResource(R.drawable.mikeon)
                        onPlay(false)
                        onRecord(true)
                    }
                    else if(text.equals("MIKE OFF"))
                    {
                        //STOP 이미지
                        micImage.setImageResource(R.drawable.mikeoff)
                        onRecord(false)
                        onPlay(true)
                        publish("SOURCE")
                   }
                } catch (e: Exception) {
                    // Give your callback on error here
                }
            }
            override fun deliveryComplete(token: IMqttDeliveryToken) {
                // Acknowledgement on delivery complete
            }
        })
    }
    fun publish(topic: String) {
        val encodedPayload : ByteArray

        var bufreader = File(fileName).inputStream()
        var data = bufreader.readBytes()
        try {
            encodedPayload = data
            val message = MqttMessage(encodedPayload)
            message.qos = 2
            message.isRetained = false
            mqttAndroidClient.publish(topic, message)
            //Toast.makeText(applicationContext,data.toString(),Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Give Callback on error here
        } catch (e: MqttException) {
            // Give Callback on error here
            Toast.makeText(applicationContext,"faile to MQTT publish",Toast.LENGTH_SHORT).show()
        }
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
