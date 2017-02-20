package me.skonb.openh264cameraview

/**
 * Created by skonb on 2017/02/20.
 */

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface

import java.util.Timer

/**
 * Created by skonb on 2013/09/30.
 */
class ProgressDialogHelper {
    internal var mProgressDialog: AlertDialog? = null
    protected var mPreviousMax: Int = 0
    internal var mTimer: Timer? = null

    fun showProgressDialog(activity: Activity?) {
        if (sShowing) return
        if (activity != null && !activity.isFinishing) {
            activity.runOnUiThread {
                if (!activity.isFinishing) {
                    hideProgressDialog(activity)
                    val dialog = ProgressDialog(activity)
                    dialog.setCancelable(false)
                    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
                    dialog.show()
                    mProgressDialog = dialog
                    if (mTimer != null) {
                        mTimer!!.cancel()

                    }
                }
            }
        }

    }


    val isProgressDialogShown: Boolean
        get() = sShowing

    fun showProgressDialogWithBar(activity: Activity?) {
        if (sShowing) return
        if (activity != null && !activity.isFinishing) {
            activity.runOnUiThread {
                if (!activity.isFinishing) {
                    hideProgressDialog(activity)
                    val progressDialog = ProgressDialog(activity)
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                    progressDialog.setCancelable(false)
                    progressDialog.max = 100
                    progressDialog.show()
                    sShowing = true
                    mProgressDialog = progressDialog
                }
            }
        }

    }

    fun showProgressDialogWithBarAndCancelButton(activity: Activity?,
                                                 cancelListener: DialogInterface.OnClickListener,
                                                 cancelMessage: String) {
        if (sShowing) return
        activity?.runOnUiThread {
            hideProgressDialog(activity)
            val progressDialog = ProgressDialog(activity)
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            progressDialog.setCancelable(false)
            progressDialog.max = 100
            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, cancelMessage,
                    cancelListener)
            progressDialog.show()
            sShowing = true
            mProgressDialog = progressDialog
        }
    }

    fun updateProgressDialogProgress(activity: Activity, max: Int, current: Int) {
        if (mProgressDialog != null) {
            if (mProgressDialog is ProgressDialog) {
                mPreviousMax = max
                (mProgressDialog as ProgressDialog).max = max
                (mProgressDialog as ProgressDialog).progress = current
            }
        }
    }

    fun updateProgressDialogMessage(activity: Activity?, message: String) {
        if (mProgressDialog != null) {
            activity?.runOnUiThread {
                if (mProgressDialog != null) {
                    mProgressDialog!!.setMessage(message)
                }
            }
        }
    }

    fun hideProgressDialog(activity: Activity?) {
        sShowing = false
        if (mProgressDialog != null) {
            if (mProgressDialog!!.isShowing) {
                activity?.runOnUiThread {
                    if (mProgressDialog != null) {
                        mProgressDialog!!.dismiss()
                        mProgressDialog = null
                    }
                }
            }
        }
    }

    fun maximizeProgress(activity: Activity?) {
        if (mProgressDialog != null) {
            activity?.runOnUiThread { (mProgressDialog as ProgressDialog).progress = mPreviousMax }
        }
    }

    companion object {
        internal var sShowing = false
    }


}

