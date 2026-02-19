# Movie Night — Android TV App Implementation Plan

## Context
Build a simple Android TV app called "Movie Night" for browsing and playing movies hosted on a personal web server. The app is intended for the user's parents, so it must be extremely simple to navigate with a TV remote. Movies are played via external players (VLC/MX Player) using Android Intents, and movie metadata is served from a lightweight Python JSON API.

---

## Phase 1: Mac Development Environment Setup

### 1.1 Install Android Studio
- Download and install Android Studio from developer.android.com
- During setup wizard: install Android SDK, Android SDK Platform-Tools, Android Emulator

### 1.2 JDK
- Android Studio bundles JBR (JetBrains Runtime), so no separate JDK install needed
- Verify with: `java -version` from Android Studio's terminal

### 1.3 Set Up Android TV Emulator
- In Android Studio → Device Manager → Create Virtual Device
- Select **TV** category → pick "Android TV (1080p)"
- Download a TV system image (API 34 recommended — Android 14)
- Launch emulator to verify it works

### 1.4 Enable ADB for Physical Device (later)
- For sideloading to a real Android TV: enable Developer Options on TV
- Connect via `adb connect <tv-ip-address>`

---

## Phase 2: Project Setup

### 2.1 Create Android TV Project in Android Studio
- **Template**: Empty Activity (Compose)
- **Package name**: `com.movienight`
- **Language**: Kotlin
- **Minimum SDK**: API 21 (Android 5.0)
- **Build system**: Kotlin DSL (Gradle)

### 2.2 Project Structure
```
MovieNight/
├── app/
│   ├── src/main/
│   │   ├── java/com/movienight/
│   │   │   ├── MainActivity.kt          # Single activity entry point
│   │   │   ├── ui/
│   │   │   │   ├── HomeScreen.kt         # Movie grid (home page)
│   │   │   │   ├── MovieCard.kt          # Single movie thumbnail card
│   │   │   │   └── theme/
│   │   │   │       └── Theme.kt          # Dark theme colors/typography
│   │   │   ├── data/
│   │   │   │   ├── MovieRepository.kt    # Fetches movies from API
│   │   │   │   ├── Movie.kt             # Data model
│   │   │   │   └── ApiService.kt        # Retrofit API interface
│   │   │   └── viewmodel/
│   │   │       └── HomeViewModel.kt      # State management for home screen
│   │   ├── AndroidManifest.xml
│   │   └── res/
│   │       ├── drawable/                 # App icons, placeholder images
│   │       └── values/                   # Strings, colors
│   ├── build.gradle.kts
│   └── ...
├── backend/                              # Python API server
│   ├── server.py                         # FastAPI server
│   ├── movies/                           # Movie files directory
│   ├── thumbnails/                       # Thumbnail images
│   └── requirements.txt
├── build.gradle.kts                      # Root gradle config
└── settings.gradle.kts
```

### 2.3 Key Dependencies
```kotlin
// Compose for TV
"androidx.tv:tv-foundation:1.0.0"
"androidx.tv:tv-material:1.0.0"

// Networking
"com.squareup.retrofit2:retrofit:2.9.0"
"com.squareup.retrofit2:converter-gson:2.9.0"

// Image Loading
"io.coil-kt:coil-compose:2.6.0"

// ViewModel + Lifecycle
"androidx.lifecycle:lifecycle-viewmodel-compose"
"androidx.activity:activity-compose"
```

---

## Phase 3: Backend — Python JSON API

### 3.1 FastAPI Server (`backend/server.py`)
- Simple FastAPI app with one endpoint
- Scans a `movies/` folder for video files
- Auto-generates movie list from filenames
- Serves thumbnails and movie files as static files

### 3.2 API Endpoint
```
GET /api/movies

Response:
[
  {
    "id": "1",
    "title": "Movie Name",
    "thumbnail_url": "http://<server-ip>:8000/thumbnails/movie-name.jpg",
    "stream_url": "http://<server-ip>:8000/movies/movie-name.mkv"
  },
  ...
]
```

### 3.3 Folder Structure on Server
```
backend/
├── server.py
├── requirements.txt        # fastapi, uvicorn
├── movies/                 # Drop movie files here
│   ├── Inception.mkv
│   └── Interstellar.mp4
└── thumbnails/             # Drop matching thumbnails here
    ├── Inception.jpg
    └── Interstellar.jpg
```

- Movie filename (without extension) becomes the movie title
- Thumbnail filename must match movie filename (different extension is ok)

---

## Phase 4: Android TV App Features

### 4.1 Home Screen (Movie Grid)
- Dark background, large movie thumbnail cards in a grid
- Movie title below each thumbnail
- D-pad navigation with clear focus indicators (highlighted border)
- Loading spinner while fetching from API
- Error state with retry button if API is unreachable
- API base URL hardcoded initially (settings screen can be added later)

### 4.2 Play Movie via External Player
- On movie card click, launch an Intent:
  ```kotlin
  val intent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(Uri.parse(movie.streamUrl), "video/*")
      putExtra("title", movie.title)
  }
  startActivity(intent)
  ```
- Opens the video in VLC, MX Player, or any installed video player
- If no player is installed, show a friendly error message

### 4.3 Resume From Last Position
- **Limitation**: External players don't reliably report back playback position
- **For now**: Simple "mark as watched" indicator on thumbnails
- **Future enhancement**: Can add built-in ExoPlayer if resume is critical

### 4.4 Navigation
- Back button on remote always returns to home screen
- Single activity architecture — simple and reliable
- Flow: Home Grid → External Player → Back button → Home Grid

---

## Phase 5: Build Order (Step-by-Step)

| Step | Task | Details |
|------|------|---------|
| 1 | Set up project | Gradle, manifest, TV dependencies |
| 2 | Build Python backend | FastAPI server with `/api/movies` |
| 3 | Build Home Screen | Movie grid with hardcoded test data |
| 4 | Connect to API | Retrofit service, ViewModel, real data |
| 5 | Add external player | Intent-based playback on click |
| 6 | Polish UI | Focus states, loading/error, app icon |
| 7 | Test & deploy | Emulator testing, sideload to real TV |

---

## Phase 6: Testing & Deployment

### Testing
- Run on Android TV emulator from Android Studio
- Install VLC for Android TV on emulator for testing playback
- Test D-pad navigation (arrow keys on emulator = D-pad)
- Test with backend running locally: `uvicorn server:app --host 0.0.0.0 --port 8000`

### Deploy to Real Android TV
```bash
# 1. Enable developer mode on Android TV:
#    Settings → About → click "Build Number" 7 times
# 2. Enable USB/Network debugging in Developer Options
# 3. Connect via network:
adb connect <android-tv-ip>:5555
# 4. Install the APK:
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Verification Checklist
- [ ] Android Studio installed and TV emulator running
- [ ] Project builds and launches on emulator
- [ ] Backend serves movie list at `http://localhost:8000/api/movies`
- [ ] App displays movie thumbnails from API on home screen
- [ ] Clicking a thumbnail opens external video player
- [ ] Back button returns to home screen
- [ ] D-pad navigation works smoothly across movie grid
- [ ] App works on real Android TV device via sideload
