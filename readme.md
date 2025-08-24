<img width="100" height="100" alt="Telegram PLAY" src="https://github.com/user-attachments/assets/632d38f5-ce06-4995-bea5-6a3d9d1ec3fe" />

# Telegram Play (Android)

📱 **Telegram Play** is a custom Android client built on **TDLib** that lets you **stream Telegram media (videos, documents, etc.) without downloading**.
It comes with an advanced in-app player powered by **ExoPlayer + FFmpeg**, supporting **smooth playback, seeking, subtitles, and multiple audio tracks**.

⚡ Designed for high-performance streaming and modern Android experiences.

---

## ✨ Features

- **TDLib integration** – fast, secure, and official Telegram library.
- **Instant streaming** of:
  - Videos
  - Document files (MP4, MKV, etc.)
  - Matroska files too (via FFmpeg decoder)
- **Advanced ExoPlayer-based player**:
  - Pinch-to-zoom video
  - Subtitle support (SRT, VTT, embedded)
  - Multi-audio track selection
  - Smooth seeking without full download
- **Custom TDLib + FFmpeg bridge** for enhanced decoding.
- Modern Android UI with optimized playback controls.

---

## 🚧 Current Limitations / Work in Progress

- ⏳ **Full File downloading** support is incomplete (needs fixes).
- 🎛️ Some **player features** are experimental and may have bugs.
- 📂 File management support not fully implemented
- 🧪 Still under active development – contributions welcome!

---

## 🛠 Tech Stack

- **[TDLib](https://github.com/tdlib/td) (Telegram Database Library)** – official C++ library for Telegram clients.
- **[ExoPlayer](https://github.com/google/ExoPlayer)** – media playback library for Android.
- **FFmpeg Decoder** – enables streaming of MPEG and extended format support.
- **Java/XML** – Android app codebase.
- **MVVM Architecture** - Android app architecture

---

## 🔧 Setup for Use

1. Download and install the APK from the [Releases](../../releases) page.
2. Get your **Telegram API credentials** from [my.telegram.org](https://my.telegram.org/) by creating a new application.
3. Enter the API credentials inside the app.
4. Log in using any phone number linked to a Telegram account (via OTP verification).
5. Open your chats and start playing videos by clicking the **Play** button.

> 🛡️ **Note:** Your Telegram API credentials are stored **only in the app’s secure local storage** and are never uploaded or shared externally.

---
## 🐞 Known Bugs / Issues

- **File downloading** feature is incomplete and may fail or crash.
- **Chats may not load correctly on first open**; restarting the app usually resolves this issue.
- Large document streaming can be slow or fail depending on file size and network conditions like Large files (more than 500 mb) do not play in IPV4 only network.
- Minor UI glitches in certain Android versions or device screen sizes.
- Close the app from the recent task too to prevent any unwanted downloading and data usage
___

## 🚀 Build the project

### Prerequisites
- Android Studio

### Build
```bash
git clone https://github.com/anuj-softech/Telegram-PLAY.git
cd Telegram-PLAY
# Open in Android Studio and configure
```

### Configuration

1. **Using a newer TDLib version (optional):**
  - Build TDLib from the source following the [official guide](https://github.com/tdlib/td).
  - Copy the compiled native libraries into:
    ```
    app/src/main/jniLibs/
    ```
  - Replace/update the Java client files (`Client.java`, `TdApi.java`) in:
    ```
    app/src/main/java/org/drinkless/tdlib/
    ```

2. **Build & run the app:**
  - Open the project in Android Studio.
  - Connect a device (or start an emulator).
  - Run the project using the desired build variant.
---

## 🤝 Contributing

Contributions are welcome! You can help with:
- Fixing **download manager** issues
- Improving **player features**
- Optimizing **streaming performance**

Please fork the repo and send a PR 🚀.

---
