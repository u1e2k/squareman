package com.retro.squareman.data.model

import android.graphics.Bitmap
import android.net.Uri

/**
 * 代表的な楽曲エンティティ
 * @param id 一意な楽曲ID
 * @param title 曲名
 * @param artist アーティスト名
 * @param album アルバム名
 * @param durationMs 再生時間（ミリ秒）
 * @param uri 再生対象URI (content:// または file://)
 * @param filePath 実ファイルパス
 * @param artwork 埋め込みジャケット画像（取得可能な場合）
 */
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri,
    val filePath: String,
    val artwork: Bitmap? = null
) {
    /**
     * 表示用のフォーマットされた再生時間（mm:ss）
     */
    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}
