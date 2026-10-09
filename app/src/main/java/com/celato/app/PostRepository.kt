package com.celato.app

import android.content.ContentResolver
import android.net.Uri
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PostRepository {

    private val db = FirebaseFirestore.getInstance()

    /** Realtime feed: nayi post / like / comment count aate hi list update hoti hai. */
    fun observePosts(): Flow<List<Post>> = callbackFlow {
        val registration = db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snap?.documents?.mapNotNull { it.toPost() } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    /** Ek user ki posts (Profile ke liye). Sorting app mein hoti hai, isliye Firestore index nahi chahiye. */
    fun observePostsByUser(uid: String): Flow<List<Post>> = callbackFlow {
        val registration = db.collection("posts")
            .whereEqualTo("authorId", uid)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { it.toPost() } ?: emptyList()
                trySend(list.sortedByDescending { it.createdAt })
            }
        awaitClose { registration.remove() }
    }

    /**
     * FREE PLAN: photo compress hokar Firestore doc ke andar (Blob) save hoti hai.
     * Baad mein upgrade par yahan Firebase Storage / Cloudflare R2 upload lagana (SETUP.md dekho).
     */
    suspend fun createPost(resolver: ContentResolver, user: User, imageUri: Uri, caption: String) {
        val bytes = withContext(Dispatchers.IO) { ImageUtils.compressToLimit(resolver, imageUri) }

        db.collection("posts").add(
            mapOf(
                "authorId" to user.id,
                "authorName" to user.name,
                "authorHandle" to user.handle,
                "caption" to caption,
                "imageBytes" to Blob.fromBytes(bytes),
                "imageUrl" to "",
                "likedBy" to emptyList<String>(),
                "comments" to 0,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun toggleLike(postId: String, uid: String, currentlyLiked: Boolean) {
        val change = if (currentlyLiked) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid)
        db.collection("posts").document(postId).update("likedBy", change).await()
    }

    // ---------- Comments ----------

    fun observeComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val registration = db.collection("posts").document(postId).collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snap?.documents?.mapNotNull { it.toComment() } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    suspend fun addComment(postId: String, user: User, text: String) {
        val postRef = db.collection("posts").document(postId)
        val commentRef = postRef.collection("comments").document()
        db.runBatch { batch ->
            batch.set(
                commentRef,
                mapOf(
                    "authorId" to user.id,
                    "authorName" to user.name,
                    "authorHandle" to user.handle,
                    "text" to text,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(postRef, "comments", FieldValue.increment(1))
        }.await()
    }

    // ---------- Mapping ----------

    private fun DocumentSnapshot.toPost(): Post? {
        val authorId = getString("authorId") ?: return null
        return Post(
            id = id,
            author = User(authorId, getString("authorName") ?: "User", getString("authorHandle") ?: "user"),
            caption = getString("caption") ?: "",
            imageUrl = getString("imageUrl") ?: "",
            likedBy = (get("likedBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            comments = (getLong("comments") ?: 0L).toInt(),
            createdAt = getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
            imageBytes = getBlob("imageBytes")?.toBytes()
        )
    }

    private fun DocumentSnapshot.toComment(): Comment? {
        val authorId = getString("authorId") ?: return null
        return Comment(
            id = id,
            author = User(authorId, getString("authorName") ?: "User", getString("authorHandle") ?: "user"),
            text = getString("text") ?: "",
            createdAt = getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis()
        )
    }
}
