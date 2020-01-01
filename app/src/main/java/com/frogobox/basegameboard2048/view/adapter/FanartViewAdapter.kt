package  com.frogobox.basegameboard2048.view.adapter

import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import  com.frogobox.basegameboard2048.base.view.BaseViewAdapter
import  com.frogobox.basegameboard2048.base.view.BaseViewHolder
import com.frogobox.basegameboard2048.model.Wallpaper
import kotlinx.android.synthetic.main.item_grid_wallpaper.view.*

/**
 * Created by Faisal Amir
 * FrogoBox Inc License
 * =========================================
 * BaseMusicPlayer
 * Copyright (C) 19/12/2019.
 * All rights reserved
 * -----------------------------------------
 * Name     : Muhammad Faisal Amir
 * E-mail   : faisalamircs@gmail.com
 * Github   : github.com/amirisback
 * LinkedIn : linkedin.com/in/faisalamircs
 * -----------------------------------------
 * FrogoBox Software Industries
 *  com.frogobox.basegameboard2048.view.adapter
 *
 */
class FanartViewAdapter : BaseViewAdapter<Wallpaper>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<Wallpaper> {
        return FashionViewHolder(viewLayout(parent))
    }

    inner class FashionViewHolder(view: View) : BaseViewHolder<Wallpaper>(view) {

        private val iv_image = view.iv_poster

        override fun initComponent(data: Wallpaper) {
            super.initComponent(data)
            Glide.with(itemView.context).load(data.linkImage).into(iv_image)
        }

    }

}