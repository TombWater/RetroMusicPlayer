/*
 * Copyright (c) 2020 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package code.name.monkey.retromusic.adapter.song

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import code.name.monkey.retromusic.model.AlbumDetailListItem
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.MusicUtil

class SimpleSongAdapter(
    context: FragmentActivity,
    items: List<AlbumDetailListItem>,
    layoutRes: Int
) : SongAdapter(context, items.toMutableList(), layoutRes) {

    override fun swapDataSet(dataSet: List<AlbumDetailListItem>) {
        this.dataSet = dataSet.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        // Specific SimpleSongAdapter logic for imageText and time has been
        // incorporated into SongAdapter's onBindViewHolder for SongItem.
        // If any distinct behavior for SimpleSongAdapter's items is needed, add here,
        // ensuring it only applies if dataSet[position] is SongItem.
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }
}
