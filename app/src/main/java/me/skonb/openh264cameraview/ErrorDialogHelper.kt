package me.skonb.openh264cameraview;

import android.app.Activity
import android.app.AlertDialog
import android.view.Gravity
import android.widget.Toast


/**
 * Created by skonb on 2013/09/30.
 */
class ErrorDialogHelper {
    fun showErrorDialogWithMessage(activity: Activity, messageRes: Int) {
        showErrorDialogWithMessage(activity, activity.getString(messageRes))
    }

    fun showErrorDialogWithMessage(activity: Activity?, message: String) {
        if (activity != null && !activity.isFinishing) {
            activity.runOnUiThread {
                val builder = AlertDialog.Builder(activity)
                builder.setMessage(message).setPositiveButton(R.string.ok) { dialog, which -> dialog.dismiss() }.show()
            }
        }

    }

    @JvmOverloads fun showMessageDialogWithMessage(activity: Activity?, message: String, title: String? = null, okButtonTitle: String? = "OK", callback: Runnable? = null) {
        if (activity != null && !activity.isFinishing) {

            activity.runOnUiThread {
                val builder = AlertDialog.Builder(activity)
                builder.setPositiveButton(okButtonTitle) { dialog, which ->
                    dialog.dismiss()
                    callback?.run()
                }.setMessage(message)
                builder.setTitle(title)
                builder.show()

            }
        }
    }

    fun showMessageDialogWithMessage(activity: Activity?, messageRes: Int) {
        showMessageDialogWithMessage(activity, messageRes, 0, R.string.ok, null)
    }

    fun showMessageDialogWithMessage(activity: Activity?, messageRes: Int, titleRes: Int, okButtonRes: Int, callback: Runnable?) {
        if (activity != null && !activity.isFinishing) {
            showMessageDialogWithMessage(activity, activity.getString(messageRes), activity.getString(titleRes), activity.getString(okButtonRes), callback)
        }

    }


    fun showNetworkErrorDialog(activity: Activity?) {
        if (activity != null && !activity.isFinishing) {
            activity.runOnUiThread {
                val toast = Toast.makeText(activity, R.string.no_network, Toast.LENGTH_LONG)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
            }

        }
    }
}
