# Contributing

This is a shorter and intro version of [README](README.md). 

# Tech Stack
- Language (JS/TS, HTML) - Kotlin, XML
- Gradle (Vite) - build tool (allows us to click "build"). Spits out build files (output) and needs to sync
- JUnit, Espresso, Mockk (Jest, Vitest) - testing, both for individual parts (unit) and systems (instrumental)
- Jetpack Compose (React) - UI library. Has animations, gestures (e.g. swipe)
- Jetpack Navigation (React Router) - Nav/route to different screens
- Material Design 3 (Tailwind/PostCSS) - Styling. Has icons
- Hilt - DI framework
- Coil - Image Loading. Browsers do it automatically, but we need to fetch/decode/cache/display images
- Proguard - don't worry about it. Basically optimizes and obfuscates the apk (app executable)

# Architecture

The main code is in app/src/main/java/dk.rosswap.mobile. Simplified tree w/o config files:
**root** is for the entire project, incl. documentation and external tools
- gradle/wrapper - gradle wrapper, builds project with the same version
- **app**/ - app stuff specifically and modules
  - build/ - gradle's build files, i.e. output from builds
  - **src/** - source code
    - test/ - unit test
    - androidTest/ - emulator test
    - **main/** - production code
      - res/ - resources, incl. icons and "drawables" (pngs, jpegs, etc)
      - **java** - for java code and not resources, which kotlin compiles to
        - **dk.rosswap.mobile** - release. All folders inside are _packages/namespaces_
          - **core** - reusable functionality
            - nav/ - routes
            - ui/ - shared ui e.g. components or themes
            - util - shared utils
          - **feature** - not reusable, e.g. auth (login, register), items (feed, create new)
            - presentation/ (UI): Screens, ViewModels
            - domain/ (business logic): Usecases, domain models, helpers
            - data/ (data persistence): repos, managers, DTOs
          - di/ (dependency injection): Auth, Firestore etc for wiring it all together

# Features

- auth (Login, Signup, LoginRequired)
- home (Home)
- items (ItemList, ItemPage, AddItem)
- liked (Liked, Disliked, Swipe)
- chat (ChatList, ChatPage)
- account (Profile)
- bugreport (BugReport)
- legal (About, Privacy, Terms)

# Nomenclature

- PascalCase - Classes, Interfaces, Objects, Enums
    - suffixes: Activity, Fragment, ViewModel, Repository, UseCase, DTO, Manager, Helper
    - [Verb+Noun] naming - use cases (e.g. LoginUserUseCase)
- camelCase - Functions, Properties/Vars, Parameters
- UPPER_SNAKE_CASE - consts
- snake_case - XMLs
- lowercase.dot.separated - modules

# Run

1. Sync with Gradle (elephant icon)
2. Assemble run configuration (hammer icon)
3. Run (run icon)

# Misc

- In Java/C# you predix interface files with I, in Kotlin the interface is just file name (e.g. AuthRepository.kt) and class is file name + Impl (AuthRepositoryImpl.kt). Impl is "default"
- DI only sends ("provides") objects as singletons to another file (a "client"). 