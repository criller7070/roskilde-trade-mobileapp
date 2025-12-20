# Tech Stack
| Purpose           | Mobile App                          | Web App                                |
|-------------------|-------------------------------------|----------------------------------------|
| Language          | Kotlin, XML                         | JavaScript, TypeScript, HTML           |
| Build Tool        | Gradle                              | Vite 6.2.0                             |
| UI Library        | Jetpack Compose                     | React 19.0.0                           |
| Styling           | Material Design 3                   | Tailwind CSS 3.3.3 + PostCSS 8.5.3     |
| Testing Framework | JUnit, Espresso, Mockk              | Jest, Vitest                           |
| Routing           | Jetpack Navigation Component        | React Router DOM 7.4.0                 |
| State Management  | ViewModel + LiveData/StateFlow      | React Context API                      |
| Backend (all)     | Android Firebase SDK                | Firebase 11.5.0                        |
| Animations        | Jetpack Compose Animation           | Framer Motion 12.18.2                  |
| Swipe Gestures    | Jetpack Compose Gestures            | react-swipeable 7.0.2                  |
| Image Loading     | Coil or Glide                       | (built-in)                             |
| Date/Time         | Java Time API                       | date-fns 4.1.0                         |
| Localization      | Android Resources (strings.xml)     | i18next 25.3.0 + react-i18next 15.5.3  |
| Icons             | Material Icons                      | Lucide React 0.514.0 + Heroicons 2.2.0 |
| Flags             | Drawable resources                  | react-world-flags 1.6.0                |
| UI Components     | Jetpack Compose Material Components | Headless UI 2.2.4                      |

# Architecture

### Layers

- **Presentation** (Compose + ViewModels): `UiState` data classes, one-way events from UI to ViewModel, Flows for lists/listeners.
- **Domain** (Use Cases + Models): Small use cases per action (CreateItem, LikeItem, SendMessage, DeleteUser, MarkRead). Domain models decoupled from Firestore DTOs.
- **Data** (Repositories + DTOs + Mappers): One repo per vertical (AuthRepo, ItemsRepo, ChatRepo, AdminRepo, FlagsRepo, BugReportsRepo). Map Firestore shapes to domain; shield UI from Firebase exceptions.

### File Structure

```text
roskilde-trade-mobileapp/
├─ app/                            # Android application module
│  ├─ .gitignore
│  ├─ build/                       # Build outputs (generated)
│  ├─ build.gradle.kts             # App module build config (applicationId/namespace/deps)
│  ├─ google-services.json         # Firebase config (per applicationId)
│  ├─ proguard-rules.pro           # R8/Proguard rules (release)
│  ├─ public/                      # App assets (images etc.)
│  └─ src/
│     ├─ main/
│     │  ├─ AndroidManifest.xml           # Manifest, i.e. metadata, versions, permissions
│     │  ├─ java/dk.rosswap.mobile/
│     │  │  ├─ App.kt                     # General App stuff + Hilt setup
│     │  │  ├─ di/                        # "Dependency Injection"
│     │  │  │  ├─ FirebaseModule.kt       # Auth/Firestore/Storage/Functions
│     │  │  │  └─ RepositoriesModule.kt   # Repos per use case, i.e. AuthRepo, ItemsRepo etc
│     │  │  ├─ core/
│     │  │  │  ├─ navigation/
│     │  │  │  │  ├─ Routes.kt
│     │  │  │  │  └─ AppNavGraph.kt        # NavHost + NavController setup/config
│     │  │  │  ├─ ui/
│     │  │  │  │  ├─ components/
│     │  │  │  │  └─ theme/
│     │  │  │  └─ util/                    # Result, validators, mappers, helpers, etc.
│     │  │  └─ feature/
│     │  │     ├─ auth/
│     │  │     │  ├─ presentation/          # screens + viewmodels + UiState
│     │  │     │  ├─ domain/                # usecases + domain models
│     │  │     │  └─ data/                  # repos + dto + firebase sources
│     │  │     ├─ items/
│     │  │     │  ├─ presentation/
│     │  │     │  ├─ domain/
│     │  │     │  └─ data/
│     │  │     ├─ chat/
│     │  │     │  ├─ presentation/
│     │  │     │  ├─ domain/
│     │  │     │  └─ data/
│     │  │     ├─ admin/
│     │  │     │  ├─ presentation/
│     │  │     │  ├─ domain/
│     │  │     │  └─ data/
│     │  │     └─ settings/
│     │  │        ├─ presentation/
│     │  │        └─ domain/
│     │  └─ res/                          # Android resources (strings, drawables, themes, etc.)
│     │     ├─ values/
│     │     └─ drawable/
│     ├─ androidTest/                     # Instrumented tests (on-device/emulator)
│     │  └─ java/dk/rosswap/mobile/
│     │     └─ ExampleInstrumentedTest.kt
│     └─ test/                            # Local unit tests (JVM). Prioritize this one.
│        └─ java/dk/rosswap/mobile/
│           └─ ExampleUnitTest.kt
├─ build.gradle.kts                # Gradle config for all modules + plugins
├─ settings.gradle.kts             # Gradle config for where to find modules
├─ gradle.properties               # Gradle/Android config for versions etc.
├─ local.properties                # Local SDK paths (not committed)
├─ gradlew / gradlew.bat           # Gradle config for where to find wrappers
├─ gradle/                         # Gradle wrappers
│  ├─ libs.versions.toml           # Centralized dependency + plugin versions (Version Catalog)
│  └─ wrapper/
│     ├─ gradle-wrapper.properties # Pins the Gradle version
│     └─ gradle-wrapper.jar        # Wrapper bootstrap
├─ LICENSE
└─ README.md
```

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
      allow read: if request.auth != null;
      allow write: if request.auth != null && (
        request.auth.uid == userId || isAdmin()
      );
    }
    
    // Server-side admin validation function
    function isAdmin() {
      return request.auth != null && 
             request.auth.token.email != null &&
             exists(/databases/$(database)/documents/admin/config) &&
             request.auth.token.email in get(/databases/$(database)/documents/admin/config).data.adminEmails;
    }
  }
}
```

### Cross Site Scripting (XSS) Protection

```csharp
// inputSanitizer.js - XSS protection and content filtering
export const sanitizeInput = (input, options = {}) => {
  if (typeof input !== 'string') return '';
  
  let sanitized = input
    .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '') // Remove scripts
    .replace(/<iframe\b[^<]*(?:(?!<\/iframe>)<[^<]*)*<\/iframe>/gi, '') // Remove iframes
    .replace(/javascript:/gi, '') // Remove javascript: URLs
    .replace(/on\w+\s*=/gi, '') // Remove event handlers
    .trim();
  
  // Content filtering for inappropriate content
  const inappropriatePatterns = [
    /\b(password|adgangskode|login|bank)\b/i,
    /\b\d{4}\s?\d{4}\s?\d{4}\s?\d{4}\b/, // Credit card patterns
    /\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b/i // Email patterns in content
  ];
  
  const containsInappropriate = inappropriatePatterns.some(pattern => 
    pattern.test(sanitized)
  );
  
  if (containsInappropriate && options.strictMode) {
    throw new Error('Indhold indeholder ikke-tilladt information');
  }
  
  return sanitized;
};
```

### Rate Limiting

```csharp
// rateLimiter.js - Client-side rate limiting
class RateLimiter {
  constructor() {
    this.actions = new Map();
  }
  
  checkLimit(actionType, userId, limits = {}) {
    const defaultLimits = {
      addItem: { max: 5, window: 300000 },           // 5 items per 5 minutes
      sendMessage: { max: 30, window: 60000 },       // 30 messages per minute
      uploadFile: { max: 10, window: 600000 },       // 10 files per 10 minutes
      flagReport: { max: 10, window: 3600000 },      // 10 flags per hour
      bugReport: { max: 5, window: 1800000 },        // 5 bug reports per 30 minutes
      adminAction: { max: 50, window: 300000 }       // 50 admin actions per 5 minutes
    };
    
    const actionLimits = { ...defaultLimits, ...limits };
    const limit = actionLimits[actionType];
    
    const key = `${actionType}_${userId}`;
    const now = Date.now();
    
    if (!this.actions.has(key)) {
      this.actions.set(key, []);
    }
    
    const actionHistory = this.actions.get(key);
    const validActions = actionHistory.filter(
      timestamp => now - timestamp < limit.window
    );
    
    if (validActions.length >= limit.max) {
      return { 
        allowed: false, 
        retryAfter: Math.ceil((validActions[0] + limit.window - now) / 1000) 
      };
    }
    
    validActions.push(now);
    this.actions.set(key, validActions);
    
    return { allowed: true };
  }
}
```

# Design

### Color Scheme
    
Front page colors (hex → rgb)

- Navbar orange (primary): orange-500 = #F97316 → rgb(249, 115, 22)
- Navbar text: white = #FFFFFF → rgb(255, 255, 255)
- Page background: orange-100 = #FFEDD5 → rgb(255, 237, 213)
- Title: text-orange-600 = #EA580C → rgb(234, 88, 12)
- Body text: text-gray-700 = #374151 → rgb(55, 65, 81)
- Placeholder bg: orange-200 = #FED7AA → rgb(254, 215, 170)
- Button hover: orange-600 = #EA580C → rgb(234, 88, 12)
