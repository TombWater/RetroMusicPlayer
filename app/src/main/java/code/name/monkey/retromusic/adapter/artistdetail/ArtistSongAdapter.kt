package code.name.monkey.retromusic.adapter.artistdetail

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.EXTRA_ALBUM_ID
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.base.AbsMultiSelectAdapter
import code.name.monkey.retromusic.adapter.base.MediaEntryViewHolder
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.asBitmapPalette
import code.name.monkey.retromusic.glide.RetroMusicColoredTarget
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.helper.menu.SongMenuHelper
import code.name.monkey.retromusic.helper.menu.SongsMenuHelper
import code.name.monkey.retromusic.model.AlbumDetailListItem
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.RetroUtil
import code.name.monkey.retromusic.util.color.MediaNotificationProcessor
import com.bumptech.glide.Glide

class ArtistSongAdapter(
    override val activity: FragmentActivity,
    var dataSet: MutableList<AlbumDetailListItem>
) : AbsMultiSelectAdapter<RecyclerView.ViewHolder, Song>(
    activity,
    R.menu.menu_media_selection
) {

    companion object {
        const val VIEW_TYPE_ALBUM_HEADER = 0
        const val VIEW_TYPE_SONG = 1
    }

    fun swapDataSet(newDataSet: List<AlbumDetailListItem>) {
        this.dataSet = ArrayList(newDataSet)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (dataSet[position]) {
            is AlbumDetailListItem.AlbumHeaderItem -> VIEW_TYPE_ALBUM_HEADER
            is AlbumDetailListItem.SongItem -> VIEW_TYPE_SONG
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_ALBUM_HEADER) {
            val view = LayoutInflater.from(activity).inflate(R.layout.item_album_header_for_song_list, parent, false)
            AlbumHeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(activity).inflate(R.layout.item_song, parent, false)
            SongItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = dataSet[position]
        when (holder) {
            is AlbumHeaderViewHolder -> {
                holder.bind(item as AlbumDetailListItem.AlbumHeaderItem)
            }
            is SongItemViewHolder -> {
                val songItem = item as AlbumDetailListItem.SongItem
                holder.bind(songItem.song, isChecked(songItem.song))
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return when (val item = dataSet[position]) {
            is AlbumDetailListItem.AlbumHeaderItem -> item.album.id * -1
            is AlbumDetailListItem.SongItem -> item.song.id
        }
    }

    override fun getItemCount(): Int = dataSet.size

    override fun getIdentifier(position: Int): Song? {
        return (dataSet.getOrNull(position) as? AlbumDetailListItem.SongItem)?.song
    }

    override fun getName(model: Song): String = model.title

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<Song>) {
        SongsMenuHelper.handleMenuClick(activity, selection, menuItem.itemId)
    }

    inner class AlbumHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val albumArt: ImageView? = itemView.findViewById(R.id.image)
        private val title: TextView? = itemView.findViewById(R.id.title)
        private val info: TextView? = itemView.findViewById(R.id.text)

        fun bind(item: AlbumDetailListItem.AlbumHeaderItem) {
            val album = item.album
            title?.text = album.title
            info?.text = activity.getString(
                R.string.album_year_and_total_duration,
                album.year.toString(),
                MusicUtil.getReadableDurationString(album.totalDuration)
            )
            albumArt?.let {
                Glide.with(activity)
                    .asBitmapPalette()
                    .songCoverOptions(album.safeGetFirstSong())
                    .load(MusicUtil.getMediaStoreAlbumCoverUri(album.id))
                    .into(object : RetroMusicColoredTarget(it) {
                        override fun onColorReady(colors: MediaNotificationProcessor) {
                            // itemView.setBackgroundColor(colors.backgroundColor)
                        }
                    })
            }
            itemView.setOnClickListener {
                 activity.findNavController(R.id.fragment_container).navigate(
                     R.id.albumDetailsFragment,
                     bundleOf(EXTRA_ALBUM_ID to album.id)
                 )
            }
        }
    }

    inner class SongItemViewHolder(itemView: View) : MediaEntryViewHolder(itemView) {
        fun bind(song: Song, isChecked: Boolean) {
            itemView.isActivated = isChecked
            this.title?.text = song.title
            this.text?.text = song.artistName

            val fixedTrackNumber = MusicUtil.getFixedTrackNumber(song.trackNumber)
            this.imageText?.text = if (fixedTrackNumber > 0) fixedTrackNumber.toString() else "-"
            this.time?.text = MusicUtil.getReadableDurationString(song.duration)

            this.image?.let {
                Glide.with(activity)
                    .asBitmapPalette()
                    .songCoverOptions(song)
                    .load(RetroGlideExtension.getSongModel(song))
                    .into(object : RetroMusicColoredTarget(it) {
                        override fun onColorReady(colors: MediaNotificationProcessor) {
                            if (paletteColorContainer != null) {
                                this@SongItemViewHolder.title?.setTextColor(colors.primaryTextColor)
                                this@SongItemViewHolder.text?.setTextColor(colors.secondaryTextColor)
                                paletteColorContainer?.setBackgroundColor(colors.backgroundColor)
                            }
                        }
                    })
            }

            menu?.isVisible = !isChecked
            if (menu?.isVisible == true) {
                menu?.setOnClickListener(object : SongMenuHelper.OnClickSongMenu(activity) {
                    override val song: Song get() = song
                    override val menuRes: Int get() = SongMenuHelper.MENU_RES
                    override fun onMenuItemClick(item: MenuItem): Boolean {
                        when (item.itemId) {
                             R.id.action_go_to_album -> {
                                 activity.findNavController(R.id.fragment_container).navigate(
                                     R.id.albumDetailsFragment,
                                     bundleOf(EXTRA_ALBUM_ID to song.albumId)
                                 )
                                 return true
                             }
                        }
                        return super.onMenuItemClick(item)
                    }
                })
            } else {
                menu?.setOnClickListener(null)
            }

            val landscape = RetroUtil.isLandscape
            if ((PreferenceUtil.songGridSize > 2 && !landscape) || (PreferenceUtil.songGridSizeLand > 5 && landscape)) {
                if (menu?.isVisible == true) {
                     menu?.isVisible = false
                     menu?.setOnClickListener(null)
                }
            }
        }

        override fun onClick(v: View?) {
            val position = layoutPosition
            if (position == RecyclerView.NO_POSITION) return
            val item = dataSet[position] as? AlbumDetailListItem.SongItem ?: return

            if (isInQuickSelectMode) {
                toggleChecked(position)
            } else {
                val songsOnly = dataSet.filterIsInstance<AlbumDetailListItem.SongItem>().map { it.song }
                val playQueuePosition = songsOnly.indexOf(item.song)
                if (playQueuePosition != -1) {
                    MusicPlayerRemote.openQueueKeepShuffleMode(songsOnly, playQueuePosition, true)
                }
            }
        }

        override fun onLongClick(v: View?): Boolean {
            val position = layoutPosition
            if (position == RecyclerView.NO_POSITION) return false
            (dataSet[position] as? AlbumDetailListItem.SongItem) ?: return false // Ensure it's a song item
            return toggleChecked(position)
        }
    }
}
