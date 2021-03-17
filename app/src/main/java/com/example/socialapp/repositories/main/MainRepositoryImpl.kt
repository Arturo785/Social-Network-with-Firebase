package com.example.socialapp.repositories.main

import android.net.Uri
import com.example.socialapp.data.entities.Comment
import com.example.socialapp.data.entities.Post
import com.example.socialapp.data.entities.ProfileUpdate
import com.example.socialapp.data.entities.User
import com.example.socialapp.other.DEFAULT_PROFILE_PICTURE_URL
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
            val chunks = uids.chunked(10)
            val resultList = mutableListOf<User>()

            chunks.forEach { chunk ->
                val usersList = users.whereIn("uid", uids).orderBy("username").get()
                    .await().toObjects(User::class.java)

                resultList.addAll(usersList)
            }

            Resource.Success(resultList)
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
                        isLiked = true
                        currentLikes + uid
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

    override suspend fun getPostForProfile(uid: String): Resource<List<Post>> = withContext(IO){
        safeCall {
            // all the posts from my profile
            val profilePosts = posts.whereEqualTo("authorUid", uid)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Post::class.java)
                .onEach { post ->
                    val user = getUser(post.authorUid).data!!
                    post.authorProfilePictureUrl = user.profilePictureUrl
                    post.authorUsername = user.username
                    post.isLiked = uid in post.likedBy // if present
                }

            Resource.Success(profilePosts)
        }
    }

    override suspend fun toggleFollowForUser(uid: String): Resource<Boolean> = withContext(IO){
        safeCall {
            var isFollowing = false

            firestore.runTransaction { transition ->
                val currentUid = auth.uid!!
                val currentUser = transition.get(users.document(currentUid)).toObject(User::class.java)!!

                // checks if we are following that person
                isFollowing = uid in currentUser.follows

                val newFollows = if (isFollowing){
                    currentUser.follows - uid
                }else{
                    currentUser.follows + uid
                }

                transition.update(users.document(currentUid), "follows", newFollows)
            }.await()

            Resource.Success(!isFollowing)
        }
    }

    override suspend fun searchUser(query: String): Resource<List<User>> = withContext(IO){
        safeCall {
            val usersResult = users.whereGreaterThanOrEqualTo("username", query.toUpperCase(Locale.ROOT))
                .get().await().toObjects(User::class.java)

            Resource.Success(usersResult)
        }
    }

    override suspend fun createComment(commentText: String, postId: String) = withContext(IO) {
            safeCall {
                val uid = auth.uid!!
                val commentId = UUID.randomUUID().toString()
                val user = getUser(uid).data!!
                val comment = Comment(
                    commentId,
                    postId,
                    uid,
                    user.username,
                    user.profilePictureUrl,
                    commentText
                )
                comments.document(commentId).set(comment).await()
                Resource.Success(comment)
            }
        }

    override suspend fun deleteComment(comment: Comment) = withContext(IO) {
        safeCall {
            comments.document(comment.commentId).delete().await()
            Resource.Success(comment)
        }
    }

    override suspend fun getCommentsForPost(postId: String) = withContext(IO){
        safeCall {
            val commentsForPost = comments
                //gets the specific post
                .whereEqualTo("postId", postId)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Comment::class.java)
                .onEach {comment ->
                    // gets the data of each comment
                    val user = getUser(comment.uid!!).data!!
                    comment.username = user.username
                    comment.profilePictureUrl = user.profilePictureUrl
                }

            Resource.Success(commentsForPost)
        }
    }

    override suspend fun updateProfile(profileUpdate: ProfileUpdate) = withContext(IO) {
        safeCall {
            val imageUrl = profileUpdate.profilePictureUri?.let { uri ->
                updateProfilePicture(profileUpdate.uidToUpdate, uri).toString()
            }
            val map = mutableMapOf(
                "username" to profileUpdate.username,
                "description" to profileUpdate.description
            )
            imageUrl?.let { url ->
                map["profilePictureUrl"] = url
            }
            users.document(profileUpdate.uidToUpdate).update(map.toMap()).await()
            Resource.Success(Any())
        }
    }

    override suspend fun updateProfilePicture(uid: String, imageUri: Uri) = withContext(IO) {
            val storageRef = storage.getReference(uid)
            val user = getUser(uid).data!!

            // not the default one
            if (user.profilePictureUrl != DEFAULT_PROFILE_PICTURE_URL) {
                storage.getReferenceFromUrl(user.profilePictureUrl).delete().await()
            }
            storageRef.putFile(imageUri).await().metadata?.reference?.downloadUrl?.await()
        }
}