package com.gichehafarm.registry.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.net.Uri
import android.content.Context
import java.io.File
import androidx.core.content.FileProvider


class PhotoViewModel : ViewModel() {
    private val _photoUri = MutableStateFlow<Uri?>(null)
    val photoUri: StateFlow<Uri?> = _photoUri

    fun setPhotoUri(uri: Uri) {
        _photoUri.value = uri
    }

    fun createImageUri(context: Context): Uri {
        val file = File(context.filesDir, "passport_photo.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun savePhotoToDatabase() {
        _photoUri.value?.let { uri ->
            // Convert URI to ByteArray and store in database
        }
    }
}
