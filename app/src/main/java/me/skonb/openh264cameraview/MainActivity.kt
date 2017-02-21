package me.skonb.openh264cameraview

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Created by skonb on 2017/02/20.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        camera_button?.setOnClickListener {
            startActivity(Intent(this, CameraRecordActivity::class.java))
        }
        surface_button?.setOnClickListener {

        }
    }
}
