package com.celato.app

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class UserProfile(val user: User, val followers: Int, val following: Int)

class UserRepository {

    private val db = FirebaseFirestore.getInstance()
    private val users = db.collection("users")

    /** Login/signup ke baad public profile doc banata hai (search ke liye zaroori). */
    suspend fun ensureUserDoc(user: User) {
        val ref = users.document(user.id)
        if (ref.get().await().exists()) return
        ref.set(
            mapOf(
                "name" to user.name,
                "handle" to user.handle,
                "nameLower" to user.name.lowercase(),
                "handleLower" to user.handle.lowercase(),
                "followersCount" to 0,
                "followingCount" to 0,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /** Khali query = naye users; warna handle ya naam ke prefix se search. */
    suspend fun searchUsers(query: String): List<UserProfile> {
        val term = query.trim().lowercase()
        if (term.isEmpty()) {
            return users.orderBy("createdAt", Query.Direction.DESCENDING).limit(20)
                .get().await().documents.mapNotNull { it.toProfile() }
        }
        val byHandle = prefixSearch("handleLower", term)
        val byName = prefixSearch("nameLower", term)
        return (byHandle + byName).distinctBy { it.user.id }
    }

    private suspend fun prefixSearch(field: String, term: String): List<UserProfile> =
        users.orderBy(field).startAt(term).endAt(term + "\uf8ff").limit(20)
            .get().await().documents.mapNotNull { it.toProfile() }

    fun observeUser(uid: String): Flow<UserProfile?> = callbackFlow {
        val registration = users.document(uid).addSnapshotListener { snap, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snap?.toProfile())
        }
        awaitClose { registration.remove() }
    }

    fun observeIsFollowing(me: String, target: String): Flow<Boolean> = callbackFlow {
        val registration = users.document(me).collection("following").document(target)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snap?.exists() == true)
            }
        awaitClose { registration.remove() }
    }

    suspend fun follow(me: String, target: String) {
        val meRef = users.document(me)
        val targetRef = users.document(target)
        db.runBatch { b ->
            b.set(meRef.collection("following").document(target), mapOf("createdAt" to FieldValue.serverTimestamp()))
            b.set(targetRef.collection("followers").document(me), mapOf("createdAt" to FieldValue.serverTimestamp()))
            b.update(meRef, "followingCount", FieldValue.increment(1))
            b.update(targetRef, "followersCount", FieldValue.increment(1))
        }.await()
    }

    suspend fun unfollow(me: String, target: String) {
        val meRef = users.document(me)
        val targetRef = users.document(target)
        db.runBatch { b ->
            b.delete(meRef.collection("following").document(target))
            b.delete(targetRef.collection("followers").document(me))
            b.update(meRef, "followingCount", FieldValue.increment(-1))
            b.update(targetRef, "followersCount", FieldValue.increment(-1))
        }.await()
    }

    private fun DocumentSnapshot.toProfile(): UserProfile? {
        val name = getString("name") ?: return null
        return UserProfile(
            user = User(id, name, getString("handle") ?: ""),
            followers = (getLong("followersCount") ?: 0L).toInt().coerceAtLeast(0),
            following = (getLong("followingCount") ?: 0L).toInt().coerceAtLeast(0)
        )
    }
}
