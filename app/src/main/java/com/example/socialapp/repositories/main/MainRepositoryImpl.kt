package com.example.socialapp.repositories.main

import android.net.Uri
import com.example.socialapp.data.entities.Post
import com.example.socialapp.data.entities.User
import com.example.socialapp.other.Resource
import com.example.socialapp.other.safeCall
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.IllegalStateException
import java.util.*

@ActivityScoped
class MainRepositoryImpl : MainRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = Firebase.storage
    private val users = firestore.collection("users")
    private val posts = firestore.collection("posts")
    private val comments = firestore.collection("comments")

    override suspend fun createPost(imageUri: Uri, text: String) = withContext(Dispatchers.IO) {
        safeCall {
            val uid = auth.uid!!
            val postId = UUID.randomUUID().toString()
            val imageUploadResult = storage.getReference(postId).putFile(imageUri).await()
            val imageUrl = imageUploadResult?.metadata?.reference?.downloadUrl?.await().toString()

            val post = Post(
                id = postId,
                authorUid = uid,
                text = text,
                imageUrl = imageUrl,
                date = System.currentTimeMillis()
            )

            posts.document(postId).set(post).await()
            Resource.Success(Any())
        }
    }

    override suspend fun getUsers(uids: List<String>): Resource<List<User>> = withContext(Dispatchers.IO){
        safeCall {
            val usersList = users.whereIn("uid", uids).orderBy("username").get()
                .await().toObjects(User::class.java)

            Resource.Success(usersList)
        }
    }

    override suspend fun getUser(uid: String): Resource<User> {
        val withContext = withContext(Dispatchers.IO) {
            // safe call has a try catch in it
            safeCall {
                val user = users.document(uid).get().await().toObject(User::class.java)
                    ?: throw IllegalStateException()

                val currentUid = FirebaseAuth.getInstance().uid!!
                val currentUser =
                    users.document(currentUid).get().await().toObject(User::class.java)
                        ?: throw IllegalStateException()

                user.isFollowing = uid in currentUser.follows

                Resource.Success(user)

            }
        }
        return withContext
    }

    override suspend fun getPostForFollows(): Resource<List<Post>> = withContext(IO){
        safeCall {
            val currentUid = FirebaseAuth.getInstance().uid!!
            val follows = getUser(currentUid).data!!.follows // gets my follows

            // all the posts from my follows
            val allPosts = posts.whereIn("authorUid", follows)
                .orderBy("date", Query.Direction.DESCENDING)
                .get().await()
                .toObjects(Post::class.java)
                .onEach { post ->
                    val user = getUser(post.authorUid).data!!
                    post.authorProfilePictureUrl = user.profilePictureUrl
                    post.authorUsername = user.username
                    post.isLiked = currentUid in post.likedBy // if present
                }

            Resource.Success(allPosts)
        }
    }

    override suspend fun toggleLikeForPost(post: Post) = withContext(Dispatchers.IO) {
        safeCall {
            var isLiked = false

            firestore.runTransaction { transaction ->
                val uid = FirebaseAuth.getInstance().uid!!
                val postResult = transaction.get(posts.document(post.id))
                val currentLikes = postResult.toObject(Post::class.java)?.likedBy ?: listOf()

                transaction.update(
                    posts.document(post.id),
                    "likedBy",
                    if(uid in currentLikes) currentLikes - uid // removes like
                    else {
                        currentLikes + uid
                        isLiked = true
                    }
                )
            }.await()

            Resource.Success(isLiked)
        }
    }

    override suspend fun deletePost(post: Post): Resource<Post> = withContext(IO){
        safeCall {
            posts.document(post.id).delete().await()
            storage.getReferenceFromUrl(post.imageUrl).delete().await()

            Resource.Success(post)
        }
    }
}