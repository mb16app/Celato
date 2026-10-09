package com.celato.app

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository : AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    /** App dobara khulne par purana login yahin se wapas milta hai. */
    override fun currentUser(): User? = auth.currentUser?.toUser()

    override suspend fun signIn(email: String, password: String): Result<User> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await().user!!.toUser()
    }

    override suspend fun signUp(name: String, email: String, password: String): Result<User> = runCatching {
        val user = auth.createUserWithEmailAndPassword(email, password).await().user!!
        user.updateProfile(userProfileChangeRequest { displayName = name }).await()
        User(user.uid, name, email.substringBefore("@"))
    }

    override fun signOut() = auth.signOut()

    private fun FirebaseUser.toUser(): User {
        val handle = (email ?: uid).substringBefore("@")
        return User(uid, displayName?.takeIf { it.isNotBlank() } ?: handle, handle)
    }
}
