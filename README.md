This is a long and detailed version of [CONTRIBUTING](CONTRIBUTING.md). 

# Tech Stack
| Purpose              | Mobile App                                             | Web App                                |
|----------------------|--------------------------------------------------------|----------------------------------------|
| Language             | Kotlin, XML                                            | JavaScript, TypeScript, HTML           |
| Build Tool           | Gradle                                                 | Vite 6.2.0                             |
| UI Library           | Android Views (Fragments) + ViewBinding (XML layouts)  | React 19.0.0                           |
| Styling              | Material Design 3                                      | Tailwind CSS 3.3.3 + PostCSS 8.5.3     |
| Testing Framework    | JUnit, Espresso, Mockk                                 | Jest, Vitest                           |
| Routing              | Jetpack Navigation Component (XML nav graph + NavHost) | React Router DOM 7.4.0                 |
| Obfuscation          | Proguard                                               | (built-in)                             |
| State Management     | ViewModel + LiveData/StateFlow                         | React Context API                      |
| Dependency Injection | Hilt                                                   | React Context API                      |
| Backend (all)        | Android Firebase SDK                                   | Firebase 11.5.0                        |
| Animations           | Android View animations                                | Framer Motion 12.18.2                  |
| Swipe Gestures       | Android touch handling / gesture detectors             | react-swipeable 7.0.2                  |
| Image Loading        | Coil                                                   | (built-in)                             |
| Localization         | Android Resources (strings.xml)                        | i18next 25.3.0 + react-i18next 15.5.3  |
| Icons                | Material Icons / Vector drawables                      | Lucide React 0.514.0 + Heroicons 2.2.0 |
| UI Components        | Material Components (Views) + XML Layouts              | Headless UI 2.2.4                      |

# Architecture

### Layers

- **Presentation** (Fragments + ViewModels): ViewBinding for XML views, one-way events from UI to ViewModel.
- **Domain** (Use Cases + Models): Small use cases per action (CreateItem, LikeItem, SendMessage, DeleteUser, MarkRead). Domain models decoupled from Firestore DTOs.
- **Data** (Repositories + DTOs + Mappers): One repo per vertical (AuthRepo, ItemsRepo, ChatRepo, AdminRepo, FlagsRepo, BugReportsRepo). Map Firestore shapes to domain; shield UI from Firebase exceptions.

### File Structure

```text
`roskilde-trade-mobileapp/`           # PROJECT ROOT
├─ `app/`                             # APP ROOT
│  ├─ `build/`                        # Gradle build files
│  ├─ `src/`                          # Source Code
│  │  ├─ `main/`                      # non-tests
│  │  │  ├─ `java/dk.rosswap.mobile/`     # APP RELEASE
│  │  │  │  ├─ `core/`                    # REUSABLE FUNCTIONALITY
│  │  │  │  │  ├─ `nav/`
│  │  │  │  │  │  ├─ `AppNavGraph.kt`     # NavHost + NavController setup/config
│  │  │  │  │  │  └─ `Routes.kt`
│  │  │  │  │  ├─ `ui/`                   # shared ui
│  │  │  │  │  │  ├─ `components/`
│  │  │  │  │  │  └─ `theme/`
│  │  │  │  │  └─ `util/`                 # shared utils
│  │  │  │  ├─ `di/`                      # WIRES TOGETHER
│  │  │  │  │  ├─ `AuthModule.kt`         # Has Repo (interface), Manager and Mapper (ORM)
│  │  │  │  │  ├─ `FirestoreModule.kt`    # can be split up. In app root per convention
│  │  │  │  │  ├─ `FunctionsModule.kt`    
│  │  │  │  │  └─ `StorageModule.kt`     
│  │  │  │  ├─ `feature/`                 # NOT REUSABLE FUNCTIONALITY
│  │  │  │  │  ├─ `admin/`                
│  │  │  │  │  │  ├─ `presentation/`      # Feature-specific UI: screens + viewmodels + UiState
│  │  │  │  │  │  ├─ `domain/`            # Business logic: usecases + domain models + helpers
│  │  │  │  │  │  └─ `data/`              # Data persistence: repos + dto + firebase sources
│  │  │  │  │  ├─ `auth/`                 # Authentication feature
│  │  │  │  │  │  ├─ `presentation/`      # Login, Signup, LoginRequired
│  │  │  │  │  │  │  ├─ `Login`           
│  │  │  │  │  │  │  ├─ `Signup`
│  │  │  │  │  │  │  └─ `LoginRequired`   # guards / auth checks
│  │  │  │  │  │  ├─ `domain/`
│  │  │  │  │  │  └─ `data/`
│  │  │  │  │  ├─ `home/`                 # Home / landing / feed
│  │  │  │  │  │  ├─ `presentation/`      # Home
│  │  │  │  │  │  ├─ `domain/`
│  │  │  │  │  │  └─ `data/`
│  │  │  │  │  ├─ `items/`                # Item discovery & management
│  │  │  │  │  │  ├─ `presentation/`      # ItemList, ItemPage, AddItem, Swipe
│  │  │  │  │  │  │  ├─ `ItemList`
│  │  │  │  │  │  │  ├─ `ItemPage`
│  │  │  │  │  │  │  ├─ `AddItem`
│  │  │  │  │  │  │  └─ `Swipe`
│  │  │  │  │  │  ├─ `domain/`
│  │  │  │  │  │  └─ `data/`
│  │  │  │  │  ├─ `liked/`                # Like / Dislike history
│  │  │  │  │  │  ├─ `presentation/`      # Liked, Disliked
│  │  │  │  │  │  │  ├─ `Liked`
│  │  │  │  │  │  │  └─ `Disliked`
│  │  │  │  │  │  ├─ `domain/`
│  │  │  │  │  │  └─ `data/`
│  │  │  │  │  ├─ `chat/`                 # Messaging
│  │  │  │  │  │  ├─ `presentation/`      # ChatList, ChatPage
│  │  │  │  │  │  │  ├─ `ChatList`
│  │  │  │  │  │  │  └─ `ChatPage`
│  │  │  │  │  │  ├─ `domain/`
│  │  │  │  │  │  └─ `data/`
│  │  │  │  │  ├─ `account/`              # Profile / account management
│  │  │  │  │  │  ├─ `presentation/`      # Profile
│  │  │  │  │  │  │  └─ `Profile`
│  │  │  │  │  │  ├─ `domain/`
│  │  │  │  │  │  └─ `data/`
│  │  │  │  │  ├─ `bugreport/`            # Bug reporting
│  │  │  │  │  │  ├─ `presentation/`      # BugReport
│  │  │  │  │  │  │  └─ `BugReport`
│  │  │  │  │  │  ├─ `domain/`
│  │  │  │  │  │  └─ `data/`
│  │  │  │  │  ├─ `legal/`                # About / Privacy / Terms
│  │  │  │  │  │  ├─ `presentation/`
│  │  │  │  │  │  │  ├─ `About`
│  │  │  │  │  │  │  ├─ `Privacy`
│  │  │  │  │  │  │  └─ `Terms`
│  │  │  │  │  │  ├─ `domain/`
│  │  │  │  │  │  └─ `data/`
│  │  │  │  │  └─ `settings/`
│  │  │  │  │     ├─ `presentation/`
│  │  │  │  │     └─ `domain/`
│  │  │  │  ├─ `App.kt`                   # App config + DI setup
│  │  │  │  └─ `MainActivity`             # main entry point
│  │  │  ├─ `res/`                    # RESOURCES
│  │  │  │  ├─ `drawable/`            # PNGs, JPEGs, ICOs
│  │  │  │  ├─ `navigation/`          # XML files for navigation
│  │  │  │  ├─ `layout/`              # XML files for UI layouts and arrangements
│  │  │  │  ├─ `menu/`                # XML files for menus
│  │  │  │  ├─ `mipmap/`              # App Launcher icons
│  │  │  │  ├─ `values/`              # XML resources that are not files, e.g. localization
│  │  │  │  └─ `xml`                  # Generic XMLs
│  │  │  └─ `AndroidManifest.xml`     # Manifest, i.e. metadata, versions, permissions
│  │  ├─ `androidTest/`               # Instrumented tests (on-device/emulator)
│  │  └─ `test/`                      # Local unit tests (JVM). Prioritize this one.
│  ├─ `build.gradle.kts`              # Gradle config for app-specific modules
│  ├─ `google-services.json`          # Firebase config
│  └─ `proguard-rules.pro`            # Proguard config
├─ `gradle/`                          # Gradle wrappers
│  ├─ `wrapper/`
│  │  ├─ `gradle-wrapper.properties`  # Pins the Gradle version for release
│  │  └─ `gradle-wrapper.jar`         # Wrapper bootstrap
│  └─ `libs.versions.toml`            # Centralized dependency + plugin versions (\`Version Catalog\`)
├─ `.gitignore`                       
├─ `build.gradle.kts`                 # Gradle config for all modules + plugins
├─ `CONTRIBUTING.md`
├─ `gradle.properties`                # Gradle/Android config for versions etc.
├─ `gradlew` / `gradlew.bat`            # Gradle config for where to find wrappers
├─ `LICENSE`
├─ `local.properties`                 # Local SDK paths (not committed)
├─ `plugin.jar`                       # Gradle plugins (not committed)
├─ `README.md`
└─ `settings.gradle.kts`              # Gradle config for where to find modules

### Resource Naming Conventions

- **Kotlin classes**: `PascalCase` (e.g. `ChatPageFragment`, `ChatListFragment`).
- **XML resources**: `snake_case`.
  - Fragment layouts: `fragment_<screen>.xml` (e.g. `fragment_chat_page.xml`, `fragment_chat_list.xml`, `fragment_account.xml`).
  - RecyclerView row layouts: `item_<thing>.xml` (e.g. `item_card.xml`, `item_chat_row.xml`, `item_message_received.xml`).
  - Navigation destinations (IDs): `nav_<screen>` (e.g. `nav_home`, `nav_chat_list`).

# Requirements

### Must Have

Authentication System

- Firebase Authentication (email/password)
- User profile management with Firestore integration
- User data fetching from Firestore on login

Trading/Discovery Engine

- Tinder-like swipe interface for item discovery
- Grid view for browsing items
- Like/Dislike functionality with history tracking
- Item creation with title, description, and image upload
- Firebase Storage integration for image uploads

Item Management

- Create items (with image upload to Firebase Storage)
- View item details
- Browse all items in grid layout
- Delete own items

Navigation & UI

- Bottom navigation or drawer navigation
- Home/landing screen
- User profile screen
- Modal/dialog system for confirmations and forms

Messaging

- Chat list with basic functionality
- Individual chat conversations
- Message history and ordering

### Should Have

Authentication System

- Google OAuth login
- Email verification support

Trading/Discovery Engine

- Fisher-Yates shuffle algorithm for randomizing items

Item Management

- View liked items history
- View disliked items history
- Flag items for content moderation

Messaging

- Firestore-backed chat system with live listeners
- Chat list with real-time updates
- Unread message counters
- Image sharing in chats
- Connection state management (online/offline/background)

Navigation & UI

- Loading placeholders for images
- About/Team screen
- Legal pages (Terms, Privacy Policy)

Content Safety & Moderation

- User flagging/reporting capability
- Input sanitization for XSS protection
- Email validation with disposable domain check

GDPR Compliance

- Complete account deletion with cascading cleanup
- Data privacy consent on signup

Data Persistence

- Local caching of items
- Offline message queueing
- Persistent chat history

### Could Have

Admin System

- Admin dashboard with statistics
- User management and deletion capability
- Item/post moderation and deletion
- Bug report management and viewing
- Content flagging resolution workflow
- Audit trail logging for admin actions
- Role-based access control (admin vs regular user)

GDPR Compliance

- User data export functionality
- Backend Cloud Functions for secure deletion

Content Safety & Moderation

- Bug reporting with optional screenshot/details
- Client-side rate limiting

Localization

- Multi-language support (same as web app)
- Configurable language switching

### Won't Have

- Algorithmic Discovery beyond Fisher-Yates
- Advanced animations and transitions beyond basic UI
- Comprehensive bug reporting system (v2+)
- Detailed audit trails for all actions
- Offline-first architecture (online-first with fallback)

# Data Models

### Object Paths

- `users/{uid}/avatar.jpg`
- `items/{itemId}/{imageId}.jpg`
- `chats/{chatId}/{messageId}/{attachmentId}`

### Common object metadata:

- `ownerId`: uid
- `visibility`: "public" | "private"
- `contentHash`: string
- `createdAt`: ISO8601 string

### **`admin/config`**

```tsx
interface AdminConfig {
  adminEmails: string[];        // List of admin email addresses
}
```

### **`users/{userId}`**

```tsx
interface User {
  uid: string;
  name: string;
  email: string;
  photoURL: string;
  createdAt: Timestamp;
  gdprConsent: boolean;           // GDPR compliance
  consentedAt: Timestamp;         // Consent timestamp
  likedItemIds?: string[];        // Swipe history
  dislikedItemIds?: string[];     // Swipe history
}
```

### **`items/{itemId}`**

```tsx
interface Item {
  id: string;
  title: string;
  description: string;
  category: 'food' | 'items' | 'services';
  price: number;
  imageUrl?: string;
  userId: string;
  userName: string;
  userPhoto?: string;
  createdAt: Timestamp;
  flagged?: boolean;             // Content moderation flag
  flagCount?: number;            // Number of user flags
}
```

### **`chats/{chatId}`**

```tsx
interface Chat {
  id: string;                    // Format: userId1_userId2_itemId
  participants: string[];        // [userId1, userId2]
  itemId: string;
  lastMessage?: string;
  lastMessageTime?: Timestamp;
  createdAt: Timestamp;
}
```

### **`messages/{messageId}`**

```tsx
interface Message {
  id: string;
  chatId: string;
  senderId: string;
  senderName: string;
  content: string;
  timestamp: Timestamp;
  read: boolean;
}

```

### **`flags/{flagId}`**

```tsx
interface Flag {
  id: string;
  itemId: string;
  reporterId: string;
  reason: 'inappropriate' | 'spam' | 'scam' | 'other';
  description?: string;
  status: 'open' | 'resolved' | 'dismissed';
  createdAt: Timestamp;
  resolvedAt?: Timestamp;
  resolvedBy?: string;           // Admin user ID
}

```

### **`bugReports/{reportId}`**

```tsx
interface BugReport {
  id: string;
  userId: string;
  userEmail: string;
  title: string;
  description: string;
  screenshotUrl?: string;
  status: 'open' | 'in-progress' | 'resolved';
  priority: 'low' | 'medium' | 'high';
  createdAt: Timestamp;
  resolvedAt?: Timestamp;
  adminNotes?: string;
}

```

### **`adminActions/{actionId}`**

```tsx
interface AdminAction {
  id: string;
  action: string;                // Action type
  adminEmail: string;
  adminUID: string;
  adminName: string;
  timestamp: Timestamp;
  details: Record<string, any>; // Action-specific details
  userAgent: string;
  ipAddress?: string;
}
```

# Security

### Content Security Policy (CSP)

```csharp
// firebase.json - comprehensive CSP
{
  "key": "Content-Security-Policy",
  "value": "default-src 'self'; script-src 'self' https://apis.google.com https://www.gstatic.com https://accounts.google.com; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data: https: blob: https://lh3.googleusercontent.com; connect-src 'self' https://firestore.googleapis.com https://firebase.googleapis.com wss://*.firebaseio.com"
}
```

### Database Rules

```csharp
// firestore.rules - Multi-layered access control
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Admin configuration (read-only via console)
    match /admin/config {
      allow read: if request.auth != null;
      allow write: if false; // Only Firebase Console can modify
    }
    
    // Admin actions audit trail
    match /adminActions/{actionId} {
      allow create: if request.auth != null && isAdmin();
      allow read: if request.auth != null && isAdmin();
      // No update/delete - audit logs are immutable
    }
    
    // Items with admin override
    match /items/{itemId} {
      allow read: if true; // Public read for discovery
      allow create: if request.auth != null && 
        request.resource.data.userId == request.auth.uid;
      allow update, delete: if request.auth != null && (
        resource.data.userId == request.auth.uid || isAdmin()
      );
    }
    
    // User profiles with admin access
    match /users/{userId} {
      allow read: if request.auth != n
