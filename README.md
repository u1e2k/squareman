# Squareman (PSPMAN Walkman Edition)

Sony Walkman および PSP 自作アプリ「PSPMAN」にインスパイアされた、**物理キー完結型のローカル音楽プレイヤーアプリ**（Android ネイティブ / Jetpack Compose）です。

Anbernic、Retroid Pocket、Miyoo、AYNなどのポータブルゲーム機型Androidデバイスや、正方形（1:1）ディスプレイ搭載機（RG Cube等）または 4:3 ディスプレイ搭載機で、画面タッチを一切行わずに十字キーと物理ボタンのみで軽快に操作できる体験を提供します。

---

## 主な特徴

1. **完全物理キーナビゲーション**:
   - タッチ操作不要。D-Pad（十字キー）と物理ボタン（ABXY, LR）だけで選曲、再生/一時停止、シーク、音量調整、画面切り替えが完結。
2. **クラシック・カセットテープ デッキ (Canvas描画)**:
   - 再生進捗率に応じて左右リールの巻き厚み（半径）が動的に変化。
   - テープ残量に反比例した回転角速度（芯に近いほど高速、巻きが太いほど低速）をリアルタイム物理シミュレーション。
   - 埋め込みアルバムアートワーク、曲名、アーティスト名、タイムコード表示。
3. **10バンド・スペクトラムアナライザ (VFD / 液晶風)**:
   - `android.media.audiofx.Visualizer` を用いた高速 FFT 解析。
   - 10バンド（31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz）に集約。
   - VFD Cyan、Retro Amber、Emerald Green、Hi-Fi Multicolor のレトロパレット切り替え対応。
   - ピークホールド表示と滑らかなアタック/ディケイ減衰。
4. **AndroidX Media3 バックグラウンド再生エンジン**:
   - `MediaSessionService` 継承の Foreground Service 実装。
   - 画面消灯時やバックグラウンド移行時も途切れない安定再生。
   - FLAC（16-bit/44.1kHz）、MP3（CBR/VBR）、WAV、OGG等のハイレゾ/ロスレス・非可逆フォーマットに対応。
   - `ExoPlayer` の `audioSessionId` を動的に Visualizer と連携。
5. **ローカル `/Music` 再帰走査 & 高速インデックス化**:
   - ストレージの `/Music` ディレクトリ配下を再帰的にスキャン。
   - `MediaMetadataRetriever` により ID3 タグや FLAC メタデータ、埋め込みアートワークを自動抽出。

---

## 物理キー操作マッピング

| ボタン / キー | 操作画面 | 動作 |
| :--- | :--- | :--- |
| **DPAD_UP / DOWN** | ライブラリ画面 | トラック選択カーソルの上下移動 |
| | プレイヤー画面 | UIフォーカス移動 |
| **DPAD_LEFT / RIGHT** | プレイヤー画面 | 巻き戻し (REW) / 早送り (FF) シーク操作 |
| | ライブラリ画面 | ページアップ / ページダウン |
| **BUTTON_L2 / R2** | 全画面共通 | 前の曲 / 次の曲へスキップ |
| **BUTTON_A / DPAD_CENTER** | ライブラリ画面 | 決定（選択トラックの再生開始・プレイヤー画面へ移行） |
| | プレイヤー画面 | 決定 / 再生・一時停止トグル |
| **BUTTON_B / BACK** | 全画面共通 | 前のビューへ戻る（カセット/スペアナからライブラリへ） |
| **BUTTON_X / BUTTON_Y** | プレイヤー画面 | デザインスタイル切り替え（`DIGITAL` ⇔ `INDEX CASSETTE` ⇔ `SKELETON`） |
| **BUTTON_L1 / R1** | 全画面共通 | 画面サイクリック切り替え（`LIBRARY` ⇔ `PLAYER` ⇔ `EQ/SPECTRUM`） |

---

## ターゲット環境・仕様

- **OS**: Android 12.0+ (Min SDK 26, Target SDK 34)
- **ディスプレイ**: 1:1 正方形ディスプレイ、4:3 レトロディスプレイ、16:9 横長ディスプレイに対応
- **推奨デバイス**:
  - Anbernic RG Cube / RG405M / RG556
  - Retroid Pocket 4 / 2S / 3+
  - 各種ポータブルゲーミングAndroid端末

---

## プロジェクト構成

```
squareman/
├── .gitignore
├── README.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── app/
    ├── build.gradle.kts
    └── src/
        └── main/
            ├── AndroidManifest.xml
            ├── res/
            │   └── values/
            │       ├── strings.xml
            │       └── themes.xml
            └── java/com/retro/squareman/
                ├── MainActivity.kt                # 物理キー捕捉・HUD・ルートUI
                ├── data/
                │   ├── model/Track.kt             # 楽曲モデル・フォーマット
                │   └── MusicScanner.kt            # /Music 再帰走査・タグ抽出
                ├── service/
                │   └── PlaybackService.kt         # MediaSessionService・バックグラウンド再生
                ├── audio/
                │   └── SpectrumVisualizer.kt      # Visualizer FFT 10バンド集約ロジック
                └── ui/
                    ├── MainViewModel.kt           # 再生・キーイベント・状態管理
                    ├── theme/
                    │   ├── Color.kt               # レトロオーディオ・VFDカラーパレット
                    │   ├── Theme.kt               # ダークテーマ定義
                    │   └── Type.kt                # モノスペース等幅フォントタイポグラフィ
                    └── components/
                        ├── CassetteDeckView.kt    # カセットテープ動的リール描画
                        ├── SpectrumAnalyzerView.kt# 10バンドVFDスペアナ描画
                        └── LibraryView.kt         # 物理キーハイライト付トラック一覧
```

---

## ビルド & インストール手順

### 1. 前提条件
- Android Studio Iguana / Jellyfish (または Android SDK 34 が利用可能な環境)
- JDK 17

### 2. ビルドコマンド
```bash
# Debug APK のビルド
./gradlew assembleDebug

# デバイスへのインストール
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. 音楽ファイルの配置
端末の外部ストレージ `/storage/emulated/0/Music` に MP3 または FLAC ファイルを転送してください。
初回起動時にストレージ読み取り権限および音声解析権限（マイク/RECORD_AUDIO）を許可すると、自動的に楽曲がインデックス化されます。
