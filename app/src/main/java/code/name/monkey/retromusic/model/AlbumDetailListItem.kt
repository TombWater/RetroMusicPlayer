package code.name.monkey.retromusic.model

sealed interface AlbumDetailListItem {
    data class AlbumHeaderItem(val album: Album) : AlbumDetailListItem
    data class SongItem(val song: Song) : AlbumDetailListItem
}
