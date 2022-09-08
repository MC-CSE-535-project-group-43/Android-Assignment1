package com.example.assignment1

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.chromium.net.CronetEngine
import org.chromium.net.UrlRequest
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : AppCompatActivity()
{
    private var currentImage: Bitmap? = null
    private var openCameraButton: Button? = null
    private var sendImageButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        openCameraButton = findViewById(R.id.button_id)
        openCameraButton?.setOnClickListener(View.OnClickListener {
            openCamera()
        })

        sendImageButton = findViewById(R.id.button_id2)
        sendImageButton?.isEnabled = false
        sendImageButton?.setOnClickListener(View.OnClickListener {
            sendImage()
        })


    }

    private fun showAlert() {
        val alertDialog = AlertDialog.Builder(this@MainActivity).create()
        alertDialog.setTitle("Alert")
        alertDialog.setMessage("App needs to access the Camera.")
        alertDialog.setButton(
            AlertDialog.BUTTON_NEGATIVE, "DONT ALLOW"
        ) { dialog, which ->
            dialog.dismiss()
            finish()
        }
        alertDialog.setButton(
            AlertDialog.BUTTON_POSITIVE, "ALLOW"
        ) { dialog, which ->
            dialog.dismiss()
            ActivityCompat.requestPermissions(
                this@MainActivity, arrayOf(Manifest.permission.CAMERA),
                MY_PERMISSIONS_REQUEST_CAMERA
            )
        }
        alertDialog.show()
    }

    private fun showSettingsAlert() {
        val alertDialog = AlertDialog.Builder(this@MainActivity).create()
        alertDialog.setTitle("Alert")
        alertDialog.setMessage("App needs to access the Camera.")
        alertDialog.setButton(
            AlertDialog.BUTTON_NEGATIVE, "DONT ALLOW"
        ) { dialog, which ->
            dialog.dismiss()
            //finish();
        }
        alertDialog.setButton(
            AlertDialog.BUTTON_POSITIVE, "SETTINGS"
        ) { dialog, which ->
            dialog.dismiss()
            startInstalledAppDetailsActivity(this@MainActivity)
        }
        alertDialog.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_CAMERA -> {
                var i = 0
                val len = permissions.size
                while (i < len) {
                    val permission = permissions[i]
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                            this, permission
                        )
                        if (showRationale) {
                            showAlert()
                        } else if (!showRationale) {
                            // user denied flagging NEVER ASK AGAIN
                            // you can either enable some fall back,
                            // disable features of your app
                            // or open another dialog explaining
                            // again the permission and directing to
                            // the app setting
                            saveToPreferences(this@MainActivity, ALLOW_KEY, true)
                        }
                    }
                    i++
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun sendImage()
    {
        if (currentImage != null)
        {
            val sir = SendImageRunnable(currentImage!!)
            Thread(sir).start()
        }
        else
        {
            sendImageButton?.isEnabled = false
        }
    }

    class SendImageRunnable(private val bitmap: Bitmap) : Runnable
    {

        public override fun run()
        {
            // Note that if you are going for a local host, you need to use the device IP
            // and not a local ip because a local ip will do to the emulators local ip
            val url = URL("https://10.0.2.2:5000/")
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection

            connection.doOutput = true
            connection.requestMethod = "POST"

            // Disable GZip
            connection.setRequestProperty("Accept-Encoding", "identity");
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos)
            connection.outputStream.write(bos.toByteArray())

            connection.connect()

            bos.flush()
            bos.close()

            connection.disconnect()
            println("DONE!")
        }
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (getFromPref(this, ALLOW_KEY)) {
                showSettingsAlert()
            } else if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                )
                != PackageManager.PERMISSION_GRANTED
            ) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.CAMERA
                    )
                ) {
                    showAlert()
                } else {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(
                        this, arrayOf(Manifest.permission.CAMERA),
                        MY_PERMISSIONS_REQUEST_CAMERA
                    )
                }
            }
        }
        else {
            val intent = Intent("android.media.action.IMAGE_CAPTURE")
            startActivityForResult(intent, 130)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == 130) {
            val imageView: ImageView = findViewById(R.id.imageView)
            currentImage = data?.extras?.get("data") as Bitmap
            imageView.setImageBitmap(currentImage)
            sendImageButton?.isEnabled = true
        }
    }

    companion object {
        const val MY_PERMISSIONS_REQUEST_CAMERA = 100
        const val ALLOW_KEY = "ALLOWED"
        const val CAMERA_PREF = "camera_pref"
        fun saveToPreferences(context: Context, key: String?, allowed: Boolean?) {
            val myPrefs = context.getSharedPreferences(
                CAMERA_PREF,
                MODE_PRIVATE
            )
            val prefsEditor = myPrefs.edit()
            prefsEditor.putBoolean(key, allowed!!)
            prefsEditor.commit()
        }

        fun getFromPref(context: Context, key: String?): Boolean {
            val myPrefs = context.getSharedPreferences(
                CAMERA_PREF,
                MODE_PRIVATE
            )
            return myPrefs.getBoolean(key, false)
        }

        fun startInstalledAppDetailsActivity(context: Activity?) {
            if (context == null) {
                return
            }
            val i = Intent()
            i.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            i.addCategory(Intent.CATEGORY_DEFAULT)
            i.data = Uri.parse("package:" + context.packageName)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            context.startActivity(i)
        }
    }
}
