## Mobile App README (Draft) — Kotlin Multiplatform (Android-first)

### Overview

- **App type**: Kotlin Multiplatform Mobile (KMM), Android-first (iOS later).
- **Core systems**:
    - Authentication and email verification
    - User profiles and privacy/GDPR
    - Items (media, status, discovery)
    - Swipes, matches, and chats
    - Reports and moderation
    - Notifications (FCM) and Remote Config
    - Analytics/logging and rate limiting
    - Data lifecycle (export/deletion/retention)

---

## Color Scheme
Front page colors (hex → rgb)
- Navbar orange (primary): orange-500 = #F97316 → rgb(249, 115, 22)
- Navbar text: white = #FFFFFF → rgb(255, 255, 255)
- Page background: orange-100 = #FFEDD5 → rgb(255, 237, 213)
- Title: text-orange-600 = #EA580C → rgb(234, 88, 12)
- Body text: text-gray-700 = #374151 → rgb(55, 65, 81)
- Placeholder bg: orange-200 = #FED7AA → rgb(254, 215, 170)
- Button hover: orange-600 = #EA580C → rgb(234, 88, 12)

## Firebase Integration

### Projects and Environments

- **Projects**
    - Dev: `ros-mobile-dev`
    - Staging: `ros-mobile-stg`
    - Prod: `ros-mobile`
- **App identifiers**
    - Android `applicationId`: `com.example.rosmobile` (dev/stg/prod variants)
    - iOS bundle ID reserved; TBD
- **Config artifacts**
    - Android: place `google-services.json` per variant in `androidApp/src/<variant>/`
    - iOS: place `GoogleService-Info.plist` per scheme (later)
- **Isolation**
    - Dedicated Firestore, Storage, Functions, and FCM per project
    - App Check enforced in staging/prod; Debug providers for dev/emulators

### Setup Checklist

- Add Firebase projects (dev/stg/prod) and download app configs.
- Enable: Auth (Email/Password), Firestore, Storage, Functions, FCM, Remote Config, App Check.
- Configure App Check: Play Integrity (Android), DeviceCheck (iOS later), Debug for dev.
- Deploy Firestore and Storage rules with staged enforcement (strict in prod).
- Deploy composite indexes (see “Indexes”).
- Deploy Cloud Functions for claims, denormalization, notifications, and lifecycle jobs.
- Configure Emulator Suite for local development (Auth/Firestore/Storage/Functions).
- Set Remote Config defaults for feature gates and rate limits.

### Firebase Services

- **Authentication**: Email/Password; enforce `email_verified` for privileged actions.
- **Firestore**: Primary data store; server timestamps; offline persistence; composite indexes.
- **Cloud Storage**: User avatars, item media, chat attachments with metadata-driven access.
- **Cloud Functions**: Custom claims, search denormalization, match creation, notifications, GDPR.
- **Cloud Messaging (FCM)**: Device token registration and targeted notifications.
- **Remote Config**: Feature flags and tunables with safe client defaults.
- **App Check**: Required in staging/prod for Auth/Firestore/Storage/Functions/FCM where supported.

---

## Authentication and Admin Authorization

### User Auth Flow

- Sign up/login via Email/Password.
- Require verified email for item creation, messaging, reporting.
- On first sign-in:
    - Create `users/{uid}` with defaults, privacy settings, and counters.

### Admin/Moderator Claims

- Custom claims on Firebase Auth:
    - `request.auth.token.admin == true`
    - `request.auth.token.moderator == true`
- Source of truth:
    - `adminUsers/{uid}` with:
        - `role`: "admin" | "moderator"
        - `grantedBy`: uid
        - `createdAt`: server timestamp
- Claims are set/cleared by backend only:
    - Trigger: write to `adminUsers/{uid}`
    - Or callable: `setClaimsForUser(uid, role)` secured by IAM/secret
- Revocation: update/delete `adminUsers/{uid}` → Function resets claims

---

## Firestore Data Model

All timestamps use `serverTimestamp()`. IDs are ULIDs or Firestore auto-IDs.

### `users/{uid}`

- `email`: string
- `displayName`: string
- `photoURL`: string | null
- `bio`: string (<= 500)
- `phone`: string | null (E.164)
- `emailVerified`: boolean
- `createdAt`: timestamp
- `updatedAt`: timestamp
- `lastActiveAt`: timestamp
- `reputation`: number
- `flagsCount`: number
- `isDisabled`: boolean
- `locale`: string
- `gdprConsent`: boolean
- `privacy`:
    - `showEmail`: boolean
    - `showPhone`: boolean
- Subcollections:
    - `fcmTokens/{token}`:
        - `platform`: "android" | "ios"
        - `createdAt`: timestamp
        - `lastSeenAt`: timestamp
        - `deviceInfo`: map
    - `bugReports/{reportId}`:
        - `title`: string
        - `description`: string
        - `appVersion`: string
        - `logsRef`: string | null
        - `createdAt`: timestamp

### `items/{itemId}`

- `ownerId`: uid
- `title`: string (<= 120)
- `description`: string (<= 2000)
- `category`: string
- `price`: number | null
- `currency`: string | null (ISO 4217)
- `condition`: "new"|"likeNew"|"used"|"forParts"
- `tags`: array<string> (<= 20)
- `location`:
    - `city`: string
    - `country`: string
    - `lat`: number
    - `lng`: number
- `media.images`: array<{
  `path`: string, `w`: number, `h`: number, `mime`: string, `blurHash`: string | null
  }>
- `status`: "active" | "paused" | "sold" | "removed"
- `likesCount`: number
- `dislikesCount`: number
- `flagsCount`: number
- `createdAt`: timestamp
- `updatedAt`: timestamp
- `search`: map (denormalized; backend-only writes)

### `swipes/{swipeId}`

- `userId`: uid
- `itemId`: itemId
- `ownerId`: uid (denormalized)
- `direction`: "like" | "dislike"
- `createdAt`: timestamp
- Deduplication key: `(userId, itemId)` enforced by Function

### `matches/{matchId}`

- `userA`: uid
- `userB`: uid
- `createdAt`: timestamp
- `lastMessageAt`: timestamp
- `active`: boolean

### `chats/{chatId}`

- `matchId`: matchId
- `members`: array<uid>[2]
- `createdAt`: timestamp
- Subcollection `messages/{messageId}`:
    - `senderId`: uid
    - `text`: string (<= 1000)
    - `attachments`: array<{
      `path`: string, `mime`: string, `w`: number | null, `h`: number | null
      }>
    - `createdAt`: timestamp
    - `editedAt`: timestamp | null
    - `deleted`: boolean
    - `readBy`: map<uid, timestamp>

### `reports/{reportId}`

- `type`: "user" | "item" | "message"
- `targetUserId`: uid | null
- `targetItemId`: itemId | null
- `targetMessageId`: messageId | null
- `reporterId`: uid
- `reason`: string (<= 500)
- `createdAt`: timestamp
- `status`: "pending" | "reviewed" | "actioned" | "dismissed"
- `reviewedBy`: uid | null
- `resolutionNote`: string | null

### `adminUsers/{uid}`

- `role`: "admin" | "moderator"
- `createdAt`: timestamp
- `grantedBy`: uid

### `stats/daily/{yyyymmdd}`

- `newUsers`: number
- `newItems`: number
- `matchesMade`: number
- `messagesSent`: number
- `reportsFiled`: number

### `flags/{flagId}`

- `targetType`: "user" | "item" | "message"
- `targetId`: string
- `flaggerId`: uid | "system"
- `reasonCode`: string
- `createdAt`: timestamp

---

## Firestore Security Rules (Outline)

Pseudocode-style to illustrate policy intent. Enforce in staging/prod.

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isAdmin() { return request.auth.token.admin == true; }
    function isModerator() { return isAdmin() || request.auth.token.moderator == true; }
    function isEmailVerified() { return request.auth.token.email_verified == true; }

    match /users/{uid} {
      allow read: if request.auth != null
                  && (uid == request.auth.uid || resource.data.privacy allows reading minimal fields);
      allow create: if request.auth != null && uid == request.auth.uid;
      allow update: if request.auth != null
                    && uid == request.auth.uid
                    && isValidUserUpdate(request.resource.data);
      allow delete: if request.auth.uid == uid || isAdmin();

      match /fcmTokens/{token} {
        allow read, write: if request.auth != null
                           && request.auth.uid == uid
                           && isValidTokenWrite();
      }

      match /bugReports/{reportId} {
        allow create: if request.auth != null && request.auth.uid == uid && isValidBugReport();
        allow read: if request.auth.uid == uid || isModerator();
      }
    }

    match /items/{itemId} {
      allow read: if true;
      allow create: if request.auth != null
                    && isEmailVerified()
                    && request.resource.data.ownerId == request.auth.uid
                    && isValidItem(request.resource.data);
      allow update, delete: if request.auth != null
                            && (request.auth.uid == resource.data.ownerId || isModerator());
    }

    match /swipes/{swipeId} {
      allow read: if request.auth != null && (request.auth.uid == resource.data.userId || isAdmin());
      allow create: if request.auth != null
                    && isEmailVerified()
                    && request.resource.data.userId == request.auth.uid
                    && isValidSwipe(request.resource.data);
      allow update, delete: if false; // immutable
    }

    match /matches/{matchId} {
      allow read, write: if request.auth != null
                         && (request.auth.uid in resource.data.members || isModerator());
    }

    match /chats/{chatId} {
      allow read, write: if request.auth != null
                         && (request.auth.uid in resource.data.members || isModerator());
      match /messages/{messageId} {
        allow create: if request.auth != null
                      && (request.auth.uid in get(/databases/$(database)/documents/chats/$(chatId)).data.members)
                      && isValidMessage(request.resource.data);
        allow read: if request.auth != null
                    && (request.auth.uid in get(/databases/$(database)/documents/chats/$(chatId)).data.members
                        || isModerator());
        allow update: if request.auth != null
                      && request.auth.uid == resource.data.senderId
                      && isEditableWindow();
        allow delete: if request.auth != null
                      && (request.auth.uid == resource.data.senderId || isModerator());
      }
    }

    match /reports/{reportId} {
      allow create: if request.auth != null
                    && isEmailVerified()
                    && isValidReport(request.resource.data);
      allow read, update: if request.auth != null
                          && (request.auth.uid == resource.data.reporterId || isModerator());
    }

    match /adminUsers/{uid} {
      allow read: if isAdmin();
      allow write: if isAdmin(); // backend sets claims on write
    }
  }
}
```

---

## Cloud Storage Layout and Rules

### Object Paths

- `users/{uid}/avatar.jpg`
- `items/{itemId}/{imageId}.jpg`
- `chats/{chatId}/{messageId}/{attachmentId}`

Common object metadata:
- `ownerId`: uid
- `visibility`: "public" | "private"
- `contentHash`: string
- `createdAt`: ISO8601 string

### Storage Rules (Outline)

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    function isSignedIn() { return request.auth != null; }
    function isAdmin() { return isSignedIn() && request.auth.token.admin == true; }
    function isModerator() { return isAdmin() || (isSignedIn() && request.auth.token.moderator == true); }

    match /users/{uid}/{fileName} {
      allow read: if isSignedIn() && (request.auth.uid == uid || profileIsPublic(uid)) || isModerator();
      allow write: if isSignedIn() && request.auth.uid == uid && validAvatar(fileName, request.resource);
    }

    match /items/{itemId}/{imageId} {
      allow read: if true;
      allow write: if isSignedIn() && isItemOwner(itemId) && validItemImage(request.resource);
    }

    match /chats/{chatId}/{messageId}/{attachmentId} {
      allow read, write: if isSignedIn() && (inChatMembers(chatId) || isModerator()) && validChatAttachment(request.resource);
    }
  }
}
```

---

## Cloud Functions Responsibilities

- **Auth/Claims**
    - `onWrite(adminUsers/{uid})`: set/unset custom claims based on `role`
    - Callable `setClaimsForUser(uid, role)`: IAM/secret-protected
- **Users**
    - `onCreate(auth.user)`: seed `users/{uid}` with defaults
    - Callable `exportUserData(uid)`: aggregate Firestore + Storage refs
    - Callable `deleteUserData(uid)`: cascade deletes and media cleanup
- **Items**
    - `onCreate(items/{itemId})`: sanitize/normalize; build `search` map; validate `media`
    - `onDelete(items/{itemId})`: delete Storage subtree
- **Swipes/Matches**
    - `onCreate(swipes/{swipeId})`: dedupe; on reciprocal like → create `matches` and `chats`
- **Chats**
    - `onCreate(messages/{messageId})`: push notification to other member; update `lastMessageAt`
- **Reports/Moderation**
    - `onCreate(reports/{reportId})`: notify admins; auto-action for critical `reasonCode`
- **Maintenance**
    - Scheduled: daily `stats` aggregation, stale cleanup, backups

---

## Indexes (Composite Examples)

```json
[
  {
    "collectionGroup": "items",
    "queryScope": "COLLECTION",
    "fields": [
      { "fieldPath": "status", "order": "ASCENDING" },
      { "fieldPath": "createdAt", "order": "DESCENDING" }
    ]
  },
  {
    "collectionGroup": "swipes",
    "queryScope": "COLLECTION",
    "fields": [
      { "fieldPath": "userId", "order": "ASCENDING" },
      { "fieldPath": "createdAt", "order": "DESCENDING" }
    ]
  },
  {
    "collectionGroup": "messages",
    "queryScope": "COLLECTION",
    "fields": [
      { "fieldPath": "chatId", "order": "ASCENDING" },
      { "fieldPath": "createdAt", "order": "ASCENDING" }
    ]
  }
]
```

---

## Notifications (FCM)

- Register tokens at `users/{uid}/fcmTokens/{token}` with `platform`, `deviceInfo`, `lastSeenAt`.
- On sign-out: delete token document; call FCM instance delete when applicable.
- Targets:
    - New message → send to other member’s active tokens
    - New match → notify both users
    - Admin action → notify affected user when appropriate
- Rate-limit bursts per user; deduplicate tokens across devices.

---

## Remote Config

- **Feature flags**: `enableMatching`, `enableReports`, etc.
- **Tunables**: `swipeDailyLimit`, `maxImagesPerItem`, `maxMessageLength`
- Client ships safe defaults; fetch+activate with timeouts; staged rollout by app version/locale.

---

## Offline, Sync, and Consistency

- Enable Firestore offline persistence with bounded cache.
- Use server timestamps for ordering; avoid client clock drift.
- Idempotent writes where possible (client-generated IDs for swipes).
- Denormalized fields (e.g., `items.search`) are backend-only and validated.

---

## Rate Limiting and Abuse Prevention

- Client hints via Remote Config thresholds.
- Server-side counters (e.g., `throttle/{uid}` daily windows) in Functions for:
    - Item creation
    - Swipes
    - Messages
    - Reports
- Reject on threshold exceed; return backoff guidance.

---

## GDPR and Privacy

- Export: callable `exportUserData(uid)` returns manifest and signed URLs.
- Deletion: callable `deleteUserData(uid)` plus Auth deletion; cascade across items, chats, tokens, and media.
- Retention: diagnostics/logs retained for limited windows; anonymize where possible.
- User controls: profile visibility flags and opt-in analytics/diagnostics.

---

## Development and Testing

- **Emulator Suite**
    - Use Auth/Firestore/Functions/Storage emulators in debug builds.
    - App configured to point to emulator hosts via environment/flags.
- **App Check**
    - Debug provider in dev; enforce in staging/prod.
- **Test Data**
    - Seed scripts for users/items/matches.
- **Secrets**
    - No secrets in client; admin actions are backend-only via IAM/claims.

---

## Operational Notes

- **Backups**
    - Daily Firestore export in prod; retention policy N days.
    - Storage lifecycle rules for orphans.
- **Media**
    - Max images per item from Remote Config.
    - Client/backend resizing and format normalization.
- **Incident Response**
    - Quarantine users/items with `isDisabled` and `status=removed`.
    - Kill-switch flags via Remote Config.

---

## Validation Constraints (Non-exhaustive)

- `title`: non-empty, <= 120, strip control chars
- `description`: <= 2000, no HTML/markup
- `tags`: unique, <= 20, each <= 30, alphanumeric plus `-`/`_`
- `price`: >= 0; requires `currency`
- `location.lat/lng`: valid ranges; required for discovery
- `message.text`: <= 1000; attachments validated
- `report.reason`: <= 500

---

## Open Questions

- ULID vs Firestore auto-ID
- Attachment size/mime whitelist
- Default avatar visibility policy
- Retention windows for logs and deleted content
- iOS KMM/App Check timeline