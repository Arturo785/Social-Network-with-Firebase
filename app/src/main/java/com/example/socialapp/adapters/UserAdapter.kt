package com.example.socialapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.example.socialapp.R
import com.example.socialapp.data.entities.Post
import com.example.socialapp.data.entities.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.item_user.view.*
import javax.inject.Inject

class UserAdapter @Inject constructor(
    private val glide : RequestManager
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>(){

    private val differCallback = object : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return  oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return  oldItem == newItem
        }
    }

    private val differ = AsyncListDiffer(this, differCallback)

    var users : List<User>
        get() = differ.currentList
        set(value) = differ.submitList(value)


    override fun getItemCount(): Int {
        return users.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_user,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]

        holder.apply {
            glide.load(user.profilePictureUrl).into(ivProfilePicture)
            tvUsername.text = user.username

            // applies it to the whole view (layout)
            itemView.setOnClickListener {
                onUserClickListener?.let {action ->
                    action(user)
                }
            }
        }

    }

    private var onUserClickListener : ((User) -> Unit)? = null

    fun setOnUserClickListener(listener : (User) -> Unit){
        onUserClickListener = listener
    }


    inner class UserViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        // this avoids unnecessary  findById calls
        val ivProfilePicture: ImageView =
            itemView.ivProfileImage // we only access once to this synthetic
        val tvUsername: TextView = itemView.tvUsername

    }

}