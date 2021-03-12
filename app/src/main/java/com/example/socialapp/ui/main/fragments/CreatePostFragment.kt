package com.example.socialapp.ui.main.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.RequestManager
import com.example.socialapp.R
import com.example.socialapp.other.EventObserver
import com.example.socialapp.ui.main.viewmodels.CreatePostViewModel
import com.example.socialapp.ui.slideUpViews
import com.example.socialapp.ui.snackbar
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_create_post.*
import javax.inject.Inject

@AndroidEntryPoint
class CreatePostFragment : Fragment(R.layout.fragment_create_post){

    @Inject // this is possible because of @AndroidEntryPoint
    lateinit var glide : RequestManager

    private val viewModel : CreatePostViewModel by viewModels()

    // same as our input type
    private lateinit var cropContent : ActivityResultLauncher<String>

    private var currentImageUri : Uri? = null

    // our anonymous object
    // used to crop the images
    private val cropActivityResultContract = object : ActivityResultContract<String, Uri?>(){
        override fun createIntent(context: Context, input: String?): Intent {
            return  CropImage.activity()
                .setAspectRatio(16,9)
                .setGuidelines(CropImageView.Guidelines.ON)
                .getIntent(requireContext())
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return CropImage.getActivityResult(intent).uri
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // we pass our anonymous object to the result of the crop
        // our activity created to crop
        cropContent = registerForActivityResult(cropActivityResultContract){
            it?.let{
                viewModel.setCurrentImageUri(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToObservers()

        btnSetPostImage.setOnClickListener {
            cropContent.launch("image/*") // any kind of image
        }

        ivPostImage.setOnClickListener {
            cropContent.launch("image/*") // any kind of image
        }

        btnPost.setOnClickListener {
            currentImageUri?.let {uri->
                viewModel.createPost(uri, etPostDescription.text.toString())
            } ?: snackbar(getString(R.string.error_no_image_chosen))
        }

        // makes a transition that we made
        slideUpViews(requireContext(), ivPostImage, btnSetPostImage, tilPostText, btnPost)
    }

    private fun subscribeToObservers() {
        viewModel.curUri.observe(viewLifecycleOwner){
            currentImageUri = it
            btnSetPostImage.isVisible = false
            glide.load(currentImageUri).into(ivPostImage)
        }


        viewModel.createPostStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                createPostProgressBar.isVisible = false
                snackbar(it)
            },
            onLoading = {
                createPostProgressBar.isVisible = true
            }
        ){
            createPostProgressBar.isVisible = false
            findNavController().popBackStack()
        })
    }
}