package com.example.socialapp.other

import java.lang.Exception

// Type T, receives a fun that returns Resource
inline fun<T> safeCall(action : () -> Resource<T>) : Resource<T>{
    return try {
        action() // calls the fun that was received
    }
    catch (e : Exception){
        return Resource.Error(message = e.message?: "Something went wrong")
    }
}