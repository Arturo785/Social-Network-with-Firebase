package com.example.socialapp.ui.main.fragments

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.RequestManager
import com.example.socialapp.R
import com.example.socialapp.adapters.PostAdapter
import com.example.socialapp.adapters.UserAdapter
import com.example.socialapp.other.EventObserver
import com.example.socialapp.ui.main.dialogs.DeletePostDialog
import com.example.socialapp.ui.main.dialogs.LikedByDialog
import com.example.socialapp.ui.main.viewmodels.BasePostViewModel
import com.example.socialapp.ui.snackbar
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


abstract class BasePostFragment(
    layoutId : Int
) : Fragment(layoutId) {

    // field injection possible because of @AndroidEntryPoint used on fragments that implements it
    @Inject
    lateinit var glide : RequestManager

    @Inject
    lateinit var postAdapter: PostAdapter

    // to hold a reference to the various progress bar that will be
    protected abstract val postProgressBar : ProgressBar
    // to implement our own viewModel
    protected abstract val basePostViewModel : BasePostViewModel

    private var curLikedIndex : Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToObservers()

        postAdapter.setOnLikeClickListener { post, i ->
            curLikedIndex = i
            post.isLiked = !post.isLiked
            basePostViewModel.toggleLikeForPost(post)
        }

        postAdapter.setOnDeleteClickListener { post ->
            //This is our own made class
            DeletePostDialog().apply {
                setPositiveListener {
                    basePostViewModel.deletePost(post)
                }
            }.show(childFragmentManager, null)
        }

        postAdapter.setOnLikedByClickListener {post ->
            basePostViewModel.getUsers(post.likedBy)
        }

        postAdapter.setOnCommentsClickListener { post ->
            findNavController().navigate(
                R.id.globalActionToCommentDialog,
                Bundle().apply {
                    putString("postId", post.id)
                }
            )
        }
    }

    private fun subscribeToObservers() {
        basePostViewModel.likePostStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                curLikedIndex?.let { index ->
                    postAdapter.posts[index].isLiking = false
                    postAdapter.notifyItemChanged(index)
                }
                snackbar(it)
            },
            onLoading = {
                curLikedIndex?.let { index ->
                    postAdapter.posts[index].isLiking = true
                    postAdapter.notifyItemChanged(index)
                }
            },
        ){ isLiked ->
            curLikedIndex?.let { index ->
                val uid = FirebaseAuth.getInstance().uid!!
                postAdapter.posts[index].apply {
                    this.isLiked = isLiked
                    isLiking = false

                    if(isLiked){
                        likedBy += uid
                    }
                    else{
                        likedBy -= uid
                    }
                }
                postAdapter.notifyItemChanged(index)
            }
        })


        basePostViewModel.deletePostStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                snackbar(it)
            },
            onLoading = {

            }
        ){deletedPost ->
            postAdapter.posts -= deletedPost
        })


        basePostViewModel.posts.observe(viewLifecycleOwner, EventObserver(
            onError = {
                postProgressBar.isVisible = false
                snackbar(it)
            },
            onLoading = {
                postProgressBar.isVisible = true
            }
        ) { posts ->
            postProgressBar.isVisible = false
            postAdapter.posts = posts
        })

        basePostViewModel.likedByUsers.observe(viewLifecycleOwner, EventObserver(
            onError = {
                snackbar(it)
            }
        ){  users ->
            val userAdapter = UserAdapter(glide)
            userAdapter.users = users
            // make the dialog
            LikedByDialog(userAdapter).show(childFragmentManager, null)
        })
    }
}