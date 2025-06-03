# Architecture

This document provides a technical overview of the application's architecture. It is intended to help new contributors understand the structure of the codebase, how different components interact, and where to find key parts of the code. This guide focuses on the technical details and assumes you have already reviewed the `CONTRIBUTING.md` for general contribution guidelines.

## Core Modules

The application is primarily composed of two modules:

*   **`app`**: This is the main Android application module. It contains all the core functionality, including UI (Activities, Fragments, Views), background services (like music playback), database management, network interactions, and business logic for the music player.
*   **`appthemehelper`**: This is a library module responsible for providing theming capabilities. It allows for dynamic theme changes (e.g., light, dark, custom colors) and ensures a consistent look and feel across the application. The `app` module utilizes this library to manage its visual presentation.

## App Module Structure

The `app` module (`app/src/main/java/code/name/monkey/retromusic/`) is organized into several key packages, each with specific technical responsibilities:

*   **`activities`**: Contains Android `Activity` classes. Activities serve as entry points for different screens or distinct flows within the app. For example, `MainActivity.kt` is the main screen the user sees, while `SettingsActivity.kt` would handle the settings screen. They are responsible for managing the overall lifecycle of a screen and hosting `Fragment`s.

*   **`adapter`**: Holds `RecyclerView.Adapter` implementations. These adapters are crucial for displaying lists of data (e.g., songs, albums, artists, playlists) in the UI. They take data from a source (often a ViewModel or Repository) and bind it to individual list item views.

*   **`appwidgets`**: This package contains the implementation for Android App Widgets (home screen widgets). It includes `AppWidgetProvider` subclasses that define the behavior and layout of different widget types offered by the app.

*   **`db`**: Contains all database-related classes, primarily for the Room persistence library. This includes:
    *   `Entity` classes: Define the schema for database tables (e.g., `SongEntity.kt`, `PlaylistEntity.kt`).
    *   `DAO` (Data Access Object) interfaces: Provide methods for interacting with the database (e.g., `SongDao.kt`, `PlaylistDao.kt`).
    *   `RetroDatabase.kt`: The main Room database class, responsible for database creation and providing access to DAOs.
    *   Migration classes: Handle database schema updates between app versions.

*   **`dialogs`**: Houses classes for creating and managing various `DialogFragment`s used throughout the app. These are used for short user interactions like creating a playlist, showing song details, or setting a sleep timer, without navigating away from the current screen.

*   **`extensions`**: Contains Kotlin extension functions. These functions extend existing classes (from the Android SDK, Kotlin standard library, or other libraries) with new utility methods, helping to write cleaner and more concise code. For example, `ViewExtensions.kt` might add helper methods for view visibility or animation.

*   **`fragments`**: Holds `Fragment` classes, which represent reusable portions of the application's UI. A single `Activity` might host multiple fragments to create a modular and flexible screen layout, especially on different screen sizes. Key fragments include those for displaying lists of songs, albums, artists, player controls, etc.

*   **`glide`**: This package integrates the Glide image loading library. It includes custom `GlideModule` implementations, `Transformation`s (e.g., for blurring images or creating rounded corners), and `Target`s to manage how images are loaded and displayed efficiently, especially for album art and artist images.

*   **`helper`**: A collection of utility classes that provide specific helper functions for various tasks. Examples include `MusicPlayerRemote.kt` for interacting with the music service, `BackupHelper.kt` for managing app data backups, and `M3UWriter.kt` for exporting playlists. These helpers encapsulate specific logic that can be reused across different parts of the app.

*   **`interfaces`**: Defines Kotlin/Java interfaces that establish contracts for communication between different components. This promotes loose coupling and allows for more flexible and testable code. For instance, an interface might define callback methods for user interactions in a list, implemented by a Fragment and invoked by an Adapter.

*   **`lyrics`**: Contains classes related to fetching, parsing, and displaying song lyrics. This might include custom views for synchronized lyrics (`LrcView.java`) and utilities for handling LRC files or other lyric formats.

*   **`model`**: Includes the data model classes (often Plain Old Kotlin Objects - POKOs or data classes) that represent the core entities of the application. Examples are `Song.kt`, `Album.kt`, `Artist.kt`, and `Playlist.kt`. These objects define the structure of the data used throughout the app, from database retrieval to UI display.

*   **`network`**: Manages network operations, such as fetching artist images, biographies, or lyrics from external APIs (e.g., Last.fm, Deezer). It likely contains Retrofit service interfaces, data classes for API responses, and potentially interceptors or utility classes for configuring network requests.

*   **`preferences`**: Handles the management of user preferences and application settings. This typically involves using Android's `SharedPreferences` or Jetpack DataStore to save and retrieve user choices, such as theme settings, playback options, and UI customizations. Custom `Preference` classes or dialogs for the settings screen are also found here.

*   **`providers`**: This package might contain `ContentProvider` implementations if the app exposes data to other applications, or more generally, classes that act as specific data providers or stores for certain types of information within the app, like `BlacklistStore.java` for managing excluded folders or `HistoryStore.java` for recently played songs.

*   **`repository`**: Implements the Repository pattern, which abstracts data sources. Repository classes (e.g., `SongRepository.kt`, `AlbumRepository.kt`) are responsible for fetching data from the local database (via DAOs) or remote network sources and providing a clean API for ViewModels or other business logic components to access this data. They handle the logic of whether to fetch from cache, database, or network.

*   **`service`**: Contains Android `Service` classes, most importantly `MusicService.kt`. This service is critical for background music playback, managing the media player instance, handling audio focus, displaying notifications with playback controls, and integrating with Android's `MediaSessionCompat` framework. Other services might handle tasks like data syncing or other background operations.

*   **`transform`**: Includes classes for UI transformations, often used with `ViewPager` or other UI elements to create custom animations and visual effects between screen transitions or item scrolling (e.g., carousel effects, depth transformations).

*   **`util`**: A general-purpose utility package containing various helper classes and functions for common tasks that don't fit into more specific packages. This can include file operations (`FileUtil.java`), image manipulation (`ImageUtil.java`), color utilities (`ColorUtil.java`), date/time formatting, and string manipulation.

*   **`views`**: Houses custom Android `View` and `ViewGroup` implementations. These are custom UI components created to achieve specific visual appearances or behaviors not available in the standard Android SDK, such as custom progress bars, themed buttons, or specialized layout containers.

*   **`volume`**: Contains classes related to observing and managing system audio volume, potentially for features like in-app volume controls or reacting to volume changes.

Some technically important classes to be aware of within these packages include:

*   `MainActivity.kt`: The main entry point for the application's UI.
*   `MusicService.kt`: The core service responsible for all music playback and media session management.
*   `RetroDatabase.kt`: The Room database instance, central to local data persistence.
*   Core model classes like `Song.kt`, `Album.kt`, `Artist.kt`: These define the fundamental data structures used throughout the app.

## Key Architectural Patterns and Concepts

The application employs several architectural patterns and concepts to maintain a structured and maintainable codebase:

*   **UI Layer**:
    *   The UI is built primarily using Android `Activities` and `Fragments`. `MainActivity.kt` often acts as a container, hosting various `Fragment`s that display different sections of the app (e.g., Library, Now Playing, Playlists).
    *   Navigation between fragments is likely handled using Android Jetpack's Navigation component or custom fragment transaction management within `MainActivity` or other host activities.
    *   `RecyclerView` is extensively used with custom `Adapter` implementations (from the `adapter` package) to display lists of music items.
    *   Data to the UI is typically provided by ViewModels (if following MVVM) or directly from Repositories, which observe data changes and update the UI accordingly.

*   **Data Layer**:
    *   **Repository Pattern**: The app uses repositories (in the `repository` package) to abstract data sources. For example, `SongRepository.kt` might fetch songs from `SongDao.kt` (for local data) or a network service (for remote data if applicable). This decouples the data access logic from the UI and business logic layers.
    *   **Room Persistence Library**: Local data storage is managed by Room (classes in the `db` package). `Entity` classes define tables, `DAO` interfaces define database operations, and `RetroDatabase.kt` serves as the database holder.
    *   **Network Operations**: For fetching data like artist bios or album art from the internet, the app likely uses libraries like Retrofit and OkHttp (configured in the `network` package). These services interact with repositories to provide data to the rest of the app.

*   **Background Playback**:
    *   The `MusicService.kt` (in the `service` package) is the cornerstone of music playback. It runs as a foreground service to ensure playback continues even when the app is not in the foreground.
    *   It manages the media player instance (e.g., `MediaPlayer` or `ExoPlayer`), handles audio focus requests, and updates playback state.
    *   `MediaSessionCompat` is used to integrate with the Android media system, allowing playback control from notifications, lock screen, Bluetooth devices, and Android Auto.
    *   The service communicates with UI components (Activities/Fragments) using mechanisms like `BroadcastReceiver`s, `LiveData`, or direct binding (if the UI binds to the service) to update playback progress, song information, and playback controls.

*   **Theming**:
    *   The `appthemehelper` module provides the core theming engine. It likely contains base styles, color palettes, and logic to switch between different themes (e.g., light, dark, black, user-defined accent colors).
    *   The `app` module uses this library to apply themes to its Activities, Fragments, and Views. This might involve setting theme resources in `Activity.onCreate()` or using themed attributes in XML layouts.
    *   Custom views (in the `views` package) may also contain logic to adapt to the current theme, ensuring a consistent appearance.

*   **Dependency Injection (DI)**:
    *   While not explicitly detailed from the file listing, modern Android apps often use a DI framework like Hilt or Koin to manage dependencies between classes.
    *   If DI is used, you would typically find setup code in the `App.kt` (Application class) or in dedicated DI modules.
    *   DI helps in decoupling components, making them easier to test and manage. For example, Repositories, DAOs, and Services would be provided as dependencies to classes that need them, rather than being instantiated directly.
    *   (Note to contributors: Further investigation into the `App.kt` class and build files might be needed to understand the specifics of DI if it's implemented.)

## Technical Guide for New Contributors

This section provides pointers on where to find code for common development tasks and understand the project's build and configuration.

*   **Modifying UI for a Specific Screen**:
    *   Identify the relevant `Activity` or `Fragment` in the `activities` or `fragments` package. For example, player UI changes would likely be in a fragment like `PlayerFragment.kt` or a specific player UI control fragment within `fragments/player/`.
    *   Layout XML files are located in `app/src/main/res/layout/`. Find the corresponding layout file for the Activity/Fragment.
    *   If dealing with lists, look at the relevant `Adapter` in the `adapter` package and its item layout XML.

*   **Changing How Data is Fetched or Processed**:
    *   Start by looking at the relevant `Repository` in the `repository` package (e.g., `SongRepository.kt`, `AlbumRepository.kt`).
    *   If changes involve local database operations, you'll need to modify the corresponding `DAO` interface in the `db` package and potentially the `Entity` class if the schema is affected.
    *   For network-related changes, look into the `network` package, particularly the Retrofit service interfaces and response models.

*   **Working with Music Playback Logic**:
    *   The primary focus will be `MusicService.kt` in the `service` package. This class handles player state, media commands, notifications, and `MediaSessionCompat` integration.
    *   Helper classes like `PlaybackManager.kt` or specific player implementations (e.g., `MultiPlayer.kt`, `CrossFadePlayer.kt`) within the `service` package are also key.

*   **Adding a New Theme or Modifying Theming Behavior**:
    *   Changes will likely involve the `appthemehelper` module. Understand its core classes and how themes are defined and applied.
    *   Within the `app` module, look for how theme attributes are used in XML styles (`app/src/main/res/values/styles.xml`) and programmatically.

*   **Understanding Build Configuration and Dependencies**:
    *   `build.gradle` (project level): Defines top-level build configurations and Gradle plugin versions.
    *   `app/build.gradle`: Contains dependencies for the `app` module, Android SDK versions, build variants, ProGuard rules, etc.
    *   `appthemehelper/build.gradle`: Dependencies and build configuration for the theming library.
    *   `settings.gradle`: Declares which modules are included in the build.
    *   `gradle/libs.versions.toml`: Centralized dependency version management.

*   **Application Manifest**:
    *   `app/src/main/AndroidManifest.xml`: This is a crucial file that declares all Activities, Services, BroadcastReceivers, ContentProviders, permissions requested by the app, hardware features, and more. Any new core component usually needs to be registered here.

*   **Debugging and Logging**:
    *   Utilize Android Studio's built-in debugger and Logcat.
    *   The app may have a utility class for logging, possibly `LogUtil.kt` in the `util` package. Check if it offers configurable log levels or tags.
