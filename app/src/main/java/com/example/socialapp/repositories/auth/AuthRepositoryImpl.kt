package com.example.socialapp.repositories.auth

import com.example.socialapp.data.entities.User
import com.example.socialapp.other.Resource
import com.example.socialapp.other.safeCall
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception

class AuthRepositoryImpl : AuthRepository {

    val auth = FirebaseAuth.getInstance()
    val users = FirebaseFirestore.getInstance().collection("users")

    // gets called by the AuthViewModel
    override suspend fun register(
        email: String,
        username: String,
        password: String
    ): Resource<AuthResult> {
        // changes the context to IO
        return withContext(Dispatchers.IO){
            // inside this is the lambda received in the safeCall fun
            safeCall {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid!!
                val user = User(uid, username)
                users.document(uid).set(user).await()

                Resource.Success(result) // is the inferred return
            }
        }
    }

    override suspend fun login(email: String, password: String): Resource<AuthResult> {
        return withContext(Dispatchers.IO) {
            safeCall {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                Resource.Success(result)
            }
        }
    }
}