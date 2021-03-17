package com.example.socialapp.ui.auth.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.socialapp.R
import com.example.socialapp.other.EventObserver
import com.example.socialapp.ui.auth.AuthViewModel
import com.example.socialapp.ui.snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_register.*


@AndroidEntryPoint
class RegisterFragment : Fragment(R.layout.fragment_register) {

    private lateinit var viewModel : AuthViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(AuthViewModel::class.java)

        subscribeToObservers()

        btnRegister.setOnClickListener {
            viewModel.register(
                etEmail.text.toString(),
                etUsername.text.toString(),
                etPassword.text.toString(),
                etRepeatPassword.text.toString()
            )
        }


        tvLogin.setOnClickListener {
            // prevents infinite adding to backStack
            if(findNavController().previousBackStackEntry != null){ // there is a fragment
                findNavController().popBackStack()
            }
            else{
                findNavController().navigate(
                    RegisterFragmentDirections.actionRegisterFragmentToLoginFragment()
                )
            }
        }

    }

  private fun subscribeToObservers(){
        viewModel.registerStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                registerProgressBar.isVisible = false
                snackbar(it)
            },
            onLoading = {
                registerProgressBar.isVisible = true
            },
            // same as the one below
       /*     onSuccess = {
                // the normal observe action
                registerProgressBar.isVisible = false
                snackbar(getString(R.string.success_registration))
            }*/

        ){
            //same as the one up
            // the normal observe action
            registerProgressBar.isVisible = false
            snackbar(getString(R.string.success_registration))
        })
    }

}