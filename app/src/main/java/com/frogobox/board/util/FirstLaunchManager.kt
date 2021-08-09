package com.frogobox.board.util

import android.content.Context
import com.frogobox.board.source.PFASQLiteHelper
import android.content.SharedPreferences
import com.frogobox.board.util.SingleConst.Pref.IS_FIRST_TIME_LAUNCH
import com.frogobox.board.util.SingleConst.Pref.PREF_NAME

class FirstLaunchManager(context: Context) {

    private val dbHandler: PFASQLiteHelper = PFASQLiteHelper(context)
    private val pref: SharedPreferences = context.getSharedPreferences(PREF_NAME, 0)
    private val editor: SharedPreferences.Editor = pref.edit()

    var isFirstTimeLaunch: Boolean
        get() = pref.getBoolean(IS_FIRST_TIME_LAUNCH, true)
        set(isFirstTime) {
            editor.putBoolean(IS_FIRST_TIME_LAUNCH, isFirstTime)
            editor.commit()
        }

    fun initFirstTimeLaunch() {
        if (pref.getBoolean(IS_FIRST_TIME_LAUNCH, true)) {
            // First time setup in here
        }
    }

}