package  com.frogobox.basegameboard2048.ui.activity

import android.os.Bundle
import  com.frogobox.basegameboard2048.R
import  com.frogobox.basegameboard2048.base.admob.BaseAdmobActivity

class AboutUsActivity : BaseAdmobActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)
        setupDetailActivity("")
    }
}
