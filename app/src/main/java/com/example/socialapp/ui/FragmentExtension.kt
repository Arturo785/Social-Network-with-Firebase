package com.example.socialapp.ui

import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar


fun Fragment.snackbar(text : String){
    Snackbar.make(
        requireView(), // comes from the fragment because we are using the class
        text,
        Snackbar.LENGTH_LONG
    ).show()
}