# Contributing

This is a shorter and intro version of [README](README.md). 

# Adding a New Feature (Kotlin Beginner-Friendly)

This guide walks you through adding a new feature if you have not used Kotlin before.

## 1) Quick Orientation

The app uses a feature-first structure with three layers:

- **Presentation**: UI (Jetpack Compose), ViewModels, and UiState
- **Domain**: Use cases, domain models, repository interfaces
- **Data**: Repository implementations, DTOs, Firebase/remote sources

Main code location:

```
app/src/main/java/dk.rosswap.mobile/
  core/           # shared utilities + navigation + UI theme/components
  di/             # dependency injection modules (Koin)
  feature/        # feature-specific code (auth, items, chat, etc)
```

Each feature usually has:

```
feature/<featureName>/
  presentation/
  domain/
  data/
```

## 2) Kotlin Primer (just enough to contribute)

### Data classes

Used for UI state and models.

```kotlin
data class ItemUiState(
  val isLoading: Boolean = false,
  val items: List<Item> = emptyList()
)
```

### Sealed classes

Used for UI events or navigation actions.

```kotlin
sealed class ItemEvent {
  data class OnItemClick(val id: String) : ItemEvent()
  object OnRefresh : ItemEvent()
}
```

### Coroutines + Flow

Used for async and reactive updates.

```kotlin
viewModelScope.launch {
  repository.getItems().collect { items ->
    _state.value = _state.value.copy(items = items)
  }
}
```

## 3) Create the Feature Skeleton

Pick a name and create a folder:

```
app/src/main/java/dk.rosswap.mobile/feature/<yourFeature>/
```

Inside, add:

```
presentation/
domain/
data/
```

## 4) Presentation Layer (UI + ViewModel)

**Screen**

`feature/<yourFeature>/presentation/<YourFeatureScreen>.kt`

```kotlin
@Composable
fun YourFeatureScreen(
  viewModel: YourFeatureViewModel = koinViewModel()
) {
  val state by viewModel.state.collectAsState()
  // build UI here
}
```

**ViewModel**

`feature/<yourFeature>/presentation/<YourFeatureViewModel>.kt`

```kotlin
class YourFeatureViewModel(
  private val fetchItems: FetchItemsUseCase
) : ViewModel() {
  private val _state = MutableStateFlow(YourFeatureUiState())
  val state: StateFlow<YourFeatureUiState> = _state

  fun onEvent(event: YourFeatureEvent) {
    // handle events
  }
}
```

**UiState + Events**

`YourFeatureUiState.kt`, `YourFeatureEvent.kt`

## 5) Domain Layer (Use Case + Interfaces)

**Use case**

`feature/<yourFeature>/domain/usecase/FetchItemsUseCase.kt`

```kotlin
class FetchItemsUseCase(
  private val repo: YourFeatureRepository
) {
  operator fun invoke(): Flow<List<Item>> = repo.getItems()
}
```

**Repository interface**

`feature/<yourFeature>/domain/repository/YourFeatureRepository.kt`

```kotlin
interface YourFeatureRepository {
  fun getItems(): Flow<List<Item>>
}
```

## 6) Data Layer (Repository Implementation)

`feature/<yourFeature>/data/repository/YourFeatureRepositoryImpl.kt`

```kotlin
class YourFeatureRepositoryImpl(
  private val firestore: FirebaseFirestore
) : YourFeatureRepository {
  override fun getItems(): Flow<List<Item>> = flow {
    // fetch data here
  }
}
```

## 7) Wire into Dependency Injection (Koin)

Add a module in `app/src/main/java/dk.rosswap.mobile/di/`:

```kotlin
val yourFeatureModule = module {
  single<YourFeatureRepository> { YourFeatureRepositoryImpl(get()) }
  factory { FetchItemsUseCase(get()) }
  viewModel { YourFeatureViewModel(get()) }
}
```

Then include the module where Koin starts (see `App.kt`).

## 8) Add Navigation Route

In `core/nav/Routes.kt`:

```kotlin
const val ROUTE_YOUR_FEATURE = "your_feature"
```

In `core/nav/AppNavGraph.kt`:

```kotlin
composable(ROUTE_YOUR_FEATURE) {
  YourFeatureScreen()
}
```

## 9) Add Strings and Resources

Add user-visible strings in:

```
app/src/main/res/values/strings.xml
```

```xml
<string name="your_feature_title">Your Feature</string>
```

## 10) (Optional) Testing

Unit tests are in:

```
app/src/test/
```

If you add domain logic, create tests there using JUnit + Mockk.

# Tech Stack
- Language (JS/TS, HTML) - Kotlin, XML
- Gradle (Vite) - build tool (allows us to click "build"). Spits out build files (output) and needs to sync
- JUnit, Espresso, Mockk (Jest, Vitest) - testing, both for individual parts (unit) and systems (instrumental)
- Jetpack Compose (React) - UI library. Has animations, gestures (e.g. swipe)
- Jetpack Navigation (React Router) - Nav/route to different screens
- Material Design 3 (Tailwind/PostCSS) - Styling. Has icons
- Koin - DI framework
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

# Run

1. Sync with Gradle (elephant icon)
2. Assemble run configuration (hammer icon)
3. Run (run icon)

# Misc

- In Java/C# you predix interface files with I, in Kotlin the interface is just file name (e.g. AuthRepository.kt) and class is file name + Impl (AuthRepositoryImpl.kt). Impl is "default"
- DI only sends ("provides") objects as singletons to another file (a "client"). 
