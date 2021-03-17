package com.example.socialapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.example.socialapp.R
import com.example.socialapp.data.entities.Post
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.item_post.view.*
import javax.inject.Inject

class PostAdapter @Inject constructor(
    private val glide : RequestManager
) : PagingDataAdapter<Post,PostAdapter.PostViewHolder>(Companion){

    companion object : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return  oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return  oldItem == newItem
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        return PostViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_post,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position) ?: return

        holder.apply {
            glide.load(post.imageUrl).into(ivPostImage)
            glide.load(post.authorProfilePictureUrl).into(ivAuthorProfileImage)
            tvPostAuthor.text = post.authorUsername
            tvPostText.text = post.text

            val likeCount = post.likedBy.size
            tvLikedBy.text = when {
                likeCount <= 0 -> "No likes"
                likeCount == 1 -> "Liked by 1 person"
                else -> "Liked by $likeCount people"
            }

            val uid = FirebaseAuth.getInstance().uid!!
            ibDeletePost.isVisible = uid == post.authorUid

            ibLike.setImageResource(
                if (post.isLiked) {
                    R.drawable.ic_like
                } else R.drawable.ic_like_white)

            tvPostAuthor.setOnClickListener {
                onUserClickListener?.let {action ->
                    action(post.authorUid)
                }
            }

            ivAuthorProfileImage.setOnClickListener {
                onUserClickListener?.let {action ->
                    action(post.authorUid)
                }
            }

            tvLikedBy.setOnClickListener {
                onLikedByListener?.let {action ->
                    action(post)
                }
            }

            ibLike.setOnClickListener {
                onLikeClickListener?.let { action ->
                    if(!post.isLiking){
                        action(post, holder.layoutPosition)
                    }
                }
            }
            ibComments.setOnClickListener {
                onCommentsClickListener?.let { action ->
                    action(post)
                }
            }
            ibDeletePost.setOnClickListener {
                onDeleteClickListener?.let { action ->
                    action(post)
                }
            }
        }
    }

    private var onLikeClickListener : ((Post, Int) -> Unit)? = null
    private var onUserClickListener : ((String) -> Unit)? = null
    private var onDeleteClickListener : ((Post) -> Unit)? = null
    private var onLikedByListener : ((Post) -> Unit)? = null
    private var onCommentsClickListener : ((Post) -> Unit)? = null

    fun setOnLikeClickListener(listener : (Post, Int) -> Unit){
        onLikeClickListener = listener
    }
    fun setOnUserClickListener(listener : (String) -> Unit){
        onUserClickListener = listener
    }
    fun setOnDeleteClickListener(listener : (Post) -> Unit){
        onDeleteClickListener = listener
    }
    fun setOnLikedByClickListener(listener : (Post) -> Unit){
        onLikedByListener = listener
    }
    fun setOnCommentsClickListener(listener : (Post) -> Unit){
        onCommentsClickListener = listener
    }



    inner class PostViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView){
        // this avoids unnecessary  findById calls
        val ivPostImage : ImageView = itemView.ivPostImage // we only access once to this synthetic
        val ivAuthorProfileImage : ImageView = itemView.ivAuthorProfileImage
        val tvPostAuthor: TextView = itemView.tvPostAuthor
        val tvPostText: TextView = itemView.tvPostText
        val tvLikedBy: TextView = itemView.tvLikedBy
        val ibLike: ImageButton = itemView.ibLike
        val ibComments: ImageButton = itemView.ibComments
        val ibDeletePost: ImageButton = itemView.ibDeletePost
    }

}