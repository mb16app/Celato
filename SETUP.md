# Celato - Setup Guide

## Firebase Setup

### 1. Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project" and name it "Celato"
3. Enable Google Analytics (optional)
4. Create the project

### 2. Add Android App to Firebase
1. In Firebase Console, click "Add app" → Android
2. Package name: `com.celato.app`
3. Debug SHA-1: Get from Android Studio or run:
   ```bash
   ./gradlew signingReport
   ```
4. Download `google-services.json` and place it in `app/` directory
5. Add Firebase SDK to `app/build.gradle.kts`:
   ```kotlin
   dependencies {
       implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
       implementation("com.google.firebase:firebase-auth")
       implementation("com.google.firebase:firebase-firestore")
       implementation("com.google.firebase:firebase-storage")
   }
   ```
6. Add plugin at top of `app/build.gradle.kts`:
   ```kotlin
   plugins {
       id("com.google.gms.google-services")
   }
   ```

### 3. Enable Firebase Services

#### Authentication
- Firebase Console → Authentication → Sign-in method
- Enable "Email/Password"

#### Firestore Database
- Firebase Console → Firestore Database → Create Database
- Start in test mode (for development)
- Region: Choose closest to you

#### Firestore Security Rules (Test Mode)
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

#### Storage (for future upgrade)
- Firebase Console → Storage → Create Bucket
- Rules (test mode):
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

---

## Cloudflare R2 Setup (Image Storage - Production)

### 1. Create Cloudflare Account
1. Go to [Cloudflare](https://dash.cloudflare.com/)
2. Sign up / Log in
3. Go to "R2" in sidebar

### 2. Create R2 Bucket
1. Click "Create Bucket"
2. Name: `celato-images`
3. Region: Auto (Cloudflare will choose optimal)
4. Create Bucket

### 3. Generate API Token
1. In R2 → "Manage R2 API tokens"
2. Click "Create API token"
3. Permission: "Object Read/Write"
4. TTL: Custom (1 year or longer)
5. Copy:
   - Access Key ID
   - Secret Access Key
   - Bucket Name
   - Account ID (from Bucket settings)
   - Subdomain (or use custom domain later)

### 4. Update PostRepository.kt

Replace the `createPost` function to upload to Cloudflare R2:

```kotlin
suspend fun createPost(resolver: ContentResolver, user: User, imageUri: Uri, caption: String) {
    val bytes = withContext(Dispatchers.IO) { ImageUtils.compressToLimit(resolver, imageUri) }
    
    // Generate unique filename
    val filename = "${user.id}_${System.currentTimeMillis()}.jpg"
    
    // Upload to Cloudflare R2
    val imageUrl = uploadToCloudflareR2(filename, bytes)
    
    db.collection("posts").add(
        mapOf(
            "authorId" to user.id,
            "authorName" to user.name,
            "authorHandle" to user.handle,
            "caption" to caption,
            "imageBytes" to null,  // No longer storing in Firestore
            "imageUrl" to imageUrl,  // Use R2 URL instead
            "likedBy" to emptyList<String>(),
            "comments" to 0,
            "createdAt" to FieldValue.serverTimestamp()
        )
    ).await()
}

private suspend fun uploadToCloudflareR2(filename: String, bytes: ByteArray): String {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .addInterceptor(BasicAuthenticator(
                ACCESS_KEY_ID,      // From R2 API token
                SECRET_ACCESS_KEY   // From R2 API token
            ))
            .build()
        
        val url = "https://${ACCOUNT_ID}.r2.cloudflarestorage.com/celato-images/$filename"
        val request = Request.Builder()
            .url(url)
            .put(RequestBody.create("image/jpeg".toMediaType(), bytes))
            .build()
        
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            "https://${SUBDOMAIN}.celato-images.pages.dev/$filename"
            // Or use: "https://r2.example.com/$filename" if using custom domain
        } else {
            throw Exception("R2 upload failed: ${response.code}")
        }
    }
}
```

### 5. Add OkHttp Dependency
In `app/build.gradle.kts`:
```kotlin
dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
}
```

### 6. Environment Variables
Create `local.properties` (git-ignored):
```properties
CLOUDFLARE_ACCOUNT_ID=your_account_id
CLOUDFLARE_ACCESS_KEY=your_access_key
CLOUDFLARE_SECRET_KEY=your_secret_key
CLOUDFLARE_R2_SUBDOMAIN=your_subdomain
```

Load in `app/build.gradle.kts`:
```kotlin
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    buildFeatures {
        buildConfig = true
    }
    
    buildTypes {
        debug {
            buildConfigField("String", "CLOUDFLARE_KEY", "\"${localProperties["CLOUDFLARE_ACCESS_KEY"]}\"")
            buildConfigField("String", "CLOUDFLARE_SECRET", "\"${localProperties["CLOUDFLARE_SECRET_KEY"]}\"")
        }
    }
}
```

---

## Firestore Database Structure

```
users/
├── {uid}
│   ├── name (string)
│   ├── handle (string)
│   ├── nameLower (string)
│   ├── handleLower (string)
│   ├── followersCount (number)
│   ├── followingCount (number)
│   ├── createdAt (timestamp)
│   ├── followers/
│   │   └── {followerId}
│   │       └── createdAt (timestamp)
│   └── following/
│       └── {targetId}
│           └── createdAt (timestamp)

posts/
├── {postId}
│   ├── authorId (string)
│   ├── authorName (string)
│   ├── authorHandle (string)
│   ├── caption (string)
│   ├── imageUrl (string) - Cloudflare R2 URL
│   ├── imageBytes (null) - Only for free tier
│   ├── likedBy (array)
│   ├── comments (number)
│   ├── createdAt (timestamp)
│   └── comments/
│       └── {commentId}
│           ├── authorId (string)
│           ├── authorName (string)
│           ├── authorHandle (string)
│           ├── text (string)
│           └── createdAt (timestamp)
```

---

## Testing Checklist

- [ ] Firebase Authentication works (Sign up / Login)
- [ ] Posts upload successfully
- [ ] Comments work
- [ ] Following/Unfollowing works
- [ ] Profile search works
- [ ] Images display correctly
- [ ] Cloudflare R2 integration (if upgraded)

---

## Production Deployment

1. **Upgrade Firebase Plan**: Switch to "Blaze" (pay-as-you-go)
2. **Enable Storage**: Use Firebase Storage or Cloudflare R2
3. **Update Security Rules**: Restrict to authenticated users
4. **Set up Billing Alerts**: Monitor Firebase costs
5. **Custom Domain**: Optional - use Cloudflare for CDN

