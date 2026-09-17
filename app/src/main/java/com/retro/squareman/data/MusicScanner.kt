package com.retro.squareman.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.retro.squareman.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MusicScanner(private val context: Context) {

    private val supportedExtensions = setOf("mp3", "flac", "wav", "ogg", "m4a")

    /**
     * 端末の /Music ディレクトリ及び MediaStore から音楽ファイルを走査・インデックス化する
     */
    suspend fun scanMusicDirectory(): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val seenPaths = mutableSetOf<String>()

        // 1. MediaStore からの高速取得
        try {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown Title"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val duration = cursor.getLong(durationCol)
                    val dataPath = cursor.getString(dataCol) ?: ""

                    if (dataPath.isNotEmpty()) {
                        seenPaths.add(dataPath)
                        val uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString())
                        val artwork = loadEmbeddedArtwork(dataPath)
                        tracks.add(
                            Track(
                                id = id,
                                title = title,
                                artist = artist,
                                album = album,
                                durationMs = duration,
                                uri = uri,
                                filePath = dataPath,
                                artwork = artwork
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. 外部ストレージの /Music ディレクトリを再帰走査（MediaStore に未登録の新規ファイル補完）
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        if (musicDir.exists() && musicDir.canRead()) {
            scanDirectoryRecursively(musicDir, seenPaths, tracks)
        }

        // 念のためSDカードや個別マウント先配下のMusicも補完
        val extDirs = context.getExternalFilesDirs(null)
        for (dir in extDirs) {
            val root = dir?.parentFile?.parentFile?.parentFile?.parentFile
            val sdMusic = File(root, "Music")
            if (sdMusic.exists() && sdMusic.canRead() && sdMusic.absolutePath != musicDir.absolutePath) {
                scanDirectoryRecursively(sdMusic, seenPaths, tracks)
            }
        }

        tracks.sortedBy { it.title.lowercase() }
    }

    private fun scanDirectoryRecursively(
        directory: File,
        seenPaths: MutableSet<String>,
        outList: MutableList<Track>
    ) {
        val files = directory.listFiles() ?: return
        val retriever = MediaMetadataRetriever()

        for (file in files) {
            if (file.isDirectory) {
                scanDirectoryRecursively(file, seenPaths, outList)
            } else if (file.isFile) {
                val ext = file.extension.lowercase()
                if (ext in supportedExtensions && file.absolutePath !in seenPaths) {
                    try {
                        retriever.setDataSource(file.absolutePath)
                        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                            ?: file.nameWithoutExtension
                        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                            ?: "Unknown Artist"
                        val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                            ?: "Unknown Album"
                        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        val duration = durationStr?.toLongOrNull() ?: 0L

                        val artwork = decodeSampledArtwork(retriever.embeddedPicture)

                        val id = file.absolutePath.hashCode().toLong()
                        val uri = Uri.fromFile(file)
                        seenPaths.add(file.absolutePath)
                        outList.add(
                            Track(
                                id = id,
                                title = title,
                                artist = artist,
                                album = album,
                                durationMs = duration,
                                uri = uri,
                                filePath = file.absolutePath,
                                artwork = artwork
                            )
                        )
                    } catch (e: Exception) {
                        // パース失敗したファイルはスキップ
                    }
                }
            }
        }
        retriever.release()
    }

    private fun loadEmbeddedArtwork(filePath: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            decodeSampledArtwork(retriever.embeddedPicture)
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    private fun decodeSampledArtwork(data: ByteArray?, targetSize: Int = 300): Bitmap? {
        if (data == null || data.isEmpty()) return null
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(data, 0, data.size, options)

            var inSampleSize = 1
            if (options.outHeight > targetSize || options.outWidth > targetSize) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= targetSize && (halfWidth / inSampleSize) >= targetSize) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeByteArray(data, 0, data.size, decodeOptions)
        } catch (e: Exception) {
            null
        }
    }
}
