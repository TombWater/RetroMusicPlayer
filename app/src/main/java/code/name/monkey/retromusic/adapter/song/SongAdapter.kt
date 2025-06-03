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

import android.content.res.ColorStateList
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import code.name.monkey.retromusic.EXTRA_ALBUM_ID
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.base.AbsMultiSelectAdapter
import code.name.monkey.retromusic.adapter.base.MediaEntryViewHolder
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.asBitmapPalette
import code.name.monkey.retromusic.glide.RetroGlideExtension.songCoverOptions
import code.name.monkey.retromusic.glide.RetroMusicColoredTarget
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.helper.SortOrder
import code.name.monkey.retromusic.helper.menu.SongMenuHelper
import code.name.monkey.retromusic.helper.menu.SongsMenuHelper
import code.name.monkey.retromusic.model.AlbumDetailListItem
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.model.Album
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.RetroUtil
import code.name.monkey.retromusic.util.color.MediaNotificationProcessor
import com.bumptech.glide.Glide
import me.zhanghai.android.fastscroll.PopupTextProvider

/**
 * Created by hemanths on 13/08/17.
 */

open class SongAdapter(
    override val activity: FragmentActivity,
    var dataSet: MutableList<AlbumDetailListItem>,
    protected var itemLayoutRes: Int,
    showSectionName: Boolean = true
) : AbsMultiSelectAdapter<SongAdapter.ViewHolder, Song>(
    activity,
    R.menu.menu_media_selection
), PopupTextProvider {

    private var showSectionName = true

    init {
        this.showSectionName = showSectionName
        this.setHasStableIds(true)
    }

    open fun swapDataSet(dataSet: List<AlbumDetailListItem>) {
        this.dataSet = ArrayList(dataSet)
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        // Return a stable ID, for songs it's song.id, for headers, maybe album.id
        // For now, let's use hashcode for headers, ensure it's stable if album data doesn't change
        // Or use a negative value of album id to distinguish from song ids if they can overlap
        return when (val item = dataSet[position]) {
            is AlbumDetailListItem.SongItem -> item.song.id
            is AlbumDetailListItem.AlbumHeaderItem -> item.album.id * -1 // Or some other stable id logic
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (dataSet[position]) {
            is AlbumDetailListItem.AlbumHeaderItem -> VIEW_TYPE_ALBUM_HEADER
            is AlbumDetailListItem.SongItem -> VIEW_TYPE_SONG
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = if (viewType == VIEW_TYPE_ALBUM_HEADER) {
            LayoutInflater.from(activity).inflate(R.layout.item_album_header_for_song_list, parent, false)
        } else { // VIEW_TYPE_SONG
            try {
                LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false)
            } catch (e: Resources.NotFoundException) {
                LayoutInflater.from(activity).inflate(R.layout.item_list, parent, false)
            }
        }
        return createViewHolder(view)
    }

    protected open fun createViewHolder(view: View): ViewHolder {
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val item = dataSet[position]) {
            is AlbumDetailListItem.AlbumHeaderItem -> {
                holder.title?.text = item.album.title // Example: Album Title (Year)
                holder.text?.text = activity.getString(R.string.album_year_and_total_duration, item.album.year.toString(), MusicUtil.getReadableDurationString(item.album.totalDuration))
                holder.menu?.isVisible = false
                holder.itemView.isActivated = false
                holder.time?.isVisible = false
                holder.imageText?.isVisible = false
                holder.image?.let { imageView ->
                    Glide.with(activity)
                        .asBitmapPalette()
                        .songCoverOptions(item.album.safeGetFirstSong()) // Using first song for styling consistency
                        .load(MusicUtil.getMediaStoreAlbumCoverUri(item.album.id)) // Correct way to load album art
                        .into(object : RetroMusicColoredTarget(imageView) {
                            override fun onColorReady(colors: MediaNotificationProcessor) {
                                // Optional: holder.paletteColorContainer?.setBackgroundColor(colors.backgroundColor)
                            }
                        })
                }
            }
            is AlbumDetailListItem.SongItem -> {
                val songFromItem = item.song // item.song is non-nullable Song here
                val isChecked = isChecked(songFromItem)
                holder.itemView.isActivated = isChecked
                // holder.menu?.isGone = isChecked // This line might be redundant if isVisible is set correctly
                holder.menu?.isVisible = !isChecked // Ensure menu is visible if not checked for songs
                holder.title?.text = getSongTitle(songFromItem)
                holder.text?.text = getSongText(songFromItem)
                loadAlbumCover(songFromItem, holder)

                holder.time?.isVisible = true
                holder.imageText?.isVisible = true
                holder.time?.text = MusicUtil.getReadableDurationString(songFromItem.duration)
                val fixedTrackNumber = MusicUtil.getFixedTrackNumber(songFromItem.trackNumber)
                holder.imageText?.text = if (fixedTrackNumber > 0) fixedTrackNumber.toString() else "-"

                if (holder.menu?.isVisible == true) { // Only set listener if menu is visible
                    holder.menu?.setOnClickListener(object : SongMenuHelper.OnClickSongMenu(activity) {
                        override val song: Song // This is now correctly a non-nullable Song
                            get() = songFromItem

                        override val menuRes: Int
                            get() = SongMenuHelper.MENU_RES

                        override fun onMenuItemClick(menuItem: MenuItem): Boolean {
                            // Pass songFromItem to the ViewHolder's method
                            return holder.onSongMenuItemClick(menuItem, songFromItem) || super.onMenuItemClick(menuItem)
                        }
                    })
                } else {
                    holder.menu?.setOnClickListener(null) // Important to clear if not visible or checked
                }

                val landscape = RetroUtil.isLandscape
                if ((PreferenceUtil.songGridSize > 2 && !landscape) || (PreferenceUtil.songGridSizeLand > 5 && landscape)) {
                    // If menu was set to visible based on !isChecked, this might override it.
                    // Consider if this grid-based visibility should also be !isChecked dependent.
                    if (holder.menu?.isVisible == true) { // only hide if it was visible
                         holder.menu?.isVisible = false
                         holder.menu?.setOnClickListener(null) // also clear listener
                    }
                }
            }
        }
    }

    private fun setColors(color: MediaNotificationProcessor, holder: ViewHolder) {
        if (holder.paletteColorContainer != null) {
            holder.title?.setTextColor(color.primaryTextColor)
            holder.text?.setTextColor(color.secondaryTextColor)
            holder.paletteColorContainer?.setBackgroundColor(color.backgroundColor)
            holder.menu?.imageTintList = ColorStateList.valueOf(color.primaryTextColor)
        }
        holder.mask?.backgroundTintList = ColorStateList.valueOf(color.primaryTextColor)
    }

    protected open fun loadAlbumCover(song: Song, holder: ViewHolder) {
        if (holder.image == null) {
            return
        }
        Glide.with(activity)
            .asBitmapPalette()
            .songCoverOptions(song)
            .load(RetroGlideExtension.getSongModel(song))
            .into(object : RetroMusicColoredTarget(holder.image!!) {
                override fun onColorReady(colors: MediaNotificationProcessor) {
                    setColors(colors, holder)
                }
            })
    }

    private fun getSongTitle(song: Song): String {
        return song.title
    }

    private fun getSongText(song: Song): String {
        return song.artistName
    }

    private fun getSongText2(song: Song): String {
        return song.albumName
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun getIdentifier(position: Int): Song? {
        return (dataSet.getOrNull(position) as? AlbumDetailListItem.SongItem)?.song
    }

    override fun getName(model: Song): String {
        return model.title
    }

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<Song>) {
        SongsMenuHelper.handleMenuClick(activity, selection, menuItem.itemId)
    }

    override fun getPopupText(position: Int): String {
        return when (val item = dataSet[position]) {
            is AlbumDetailListItem.AlbumHeaderItem -> MusicUtil.getSectionName(item.album.title)
            is AlbumDetailListItem.SongItem -> {
                val song = item.song
                when (PreferenceUtil.songSortOrder) { // Assuming songSortOrder is still relevant for individual song popups
                    SortOrder.SongSortOrder.SONG_DEFAULT -> MusicUtil.getSectionName(song.title, true)
                    SortOrder.SongSortOrder.SONG_A_Z, SortOrder.SongSortOrder.SONG_Z_A -> song.title
                    SortOrder.SongSortOrder.SONG_ALBUM -> song.albumName
                    SortOrder.SongSortOrder.SONG_ARTIST -> song.artistName
                    SortOrder.SongSortOrder.SONG_YEAR -> MusicUtil.getYearString(song.year)
                    SortOrder.SongSortOrder.COMPOSER -> song.composer ?: ""
                    SortOrder.SongSortOrder.SONG_ALBUM_ARTIST -> song.albumArtist ?: ""
                    else -> ""
                }
            }
        }
    }

    open inner class ViewHolder(itemView: View) : MediaEntryViewHolder(itemView) {
        // protected open var songMenuRes = SongMenuHelper.MENU_RES // Removed as it's directly used or passed
        protected open val currentSong: Song? // Renamed to avoid confusion with the 'song' in OnClickSongMenu
            get() = (dataSet.getOrNull(layoutPosition) as? AlbumDetailListItem.SongItem)?.song

        init {
            // Menu click listener is now set in onBindViewHolder
            // itemView.setOnClickListener(this) // Already handled by MediaEntryViewHolder
            // itemView.setOnLongClickListener(this) // Already handled by MediaEntryViewHolder
        }

        // Modified to accept Song parameter
        open fun onSongMenuItemClick(item: MenuItem, song: Song): Boolean {
            if (image != null && image!!.isVisible) { // song parameter is non-null here
                when (item.itemId) {
                    R.id.action_go_to_album -> {
                        activity.findNavController(R.id.fragment_container)
                            .navigate(
                                R.id.albumDetailsFragment,
                                bundleOf(EXTRA_ALBUM_ID to song.albumId)
                            )
                        return true
                    }
                }
            }
            return false
        }

        override fun onClick(v: View?) {
            val item = dataSet.getOrNull(layoutPosition) ?: return
            if (item is AlbumDetailListItem.SongItem) {
                if (isInQuickSelectMode) {
                    toggleChecked(layoutPosition)
                } else {
                    val songsOnly = dataSet.filterIsInstance<AlbumDetailListItem.SongItem>().map { it.song }
                    val currentSong = item.song
                    val playQueuePosition = songsOnly.indexOf(currentSong)
                    if (playQueuePosition != -1) {
                         MusicPlayerRemote.openQueueKeepShuffleMode(songsOnly, playQueuePosition, true)
                    }
                }
            } else if (item is AlbumDetailListItem.AlbumHeaderItem) {
                val album = item.album
                activity.findNavController(R.id.fragment_container)
                   .navigate(
                       R.id.albumDetailsFragment,
                       bundleOf(EXTRA_ALBUM_ID to album.id)
                   )
            }
        }

        override fun onLongClick(v: View?): Boolean {
            if (dataSet.getOrNull(layoutPosition) is AlbumDetailListItem.SongItem) {
                 return toggleChecked(layoutPosition)
            }
            return false
        }
    }

    companion object {
        const val VIEW_TYPE_ALBUM_HEADER = 0
        const val VIEW_TYPE_SONG = 1
        val TAG: String = SongAdapter::class.java.simpleName
    }
}
