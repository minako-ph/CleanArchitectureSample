package com.finc.cleanarchtecturesample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModel

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}


// All classes are here for clarify

// This is Data Transfer Object, which means just an entity to identify an instance.
// Mainly used as de-serialized model. (from json to each platform)
data class UserDTO(val id: String, val firstName: String, val lastName: String)

data class User(val displayName: String)

interface UserRepository {
    fun getUser(id: String)
    fun getAllUsers()
}
