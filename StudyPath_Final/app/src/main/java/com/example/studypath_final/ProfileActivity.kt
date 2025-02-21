package com.example.studypath_final

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.studypath_final.databinding.ActivityProfileBinding
import com.example.studypath_final.imgur.ImgurResponse
import com.example.studypath_final.imgur.RetrofitClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import java.io.ByteArrayOutputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        fetchUserData()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnChangePicture.setOnClickListener {
            showImagePickerDialog()
        }
    }

    private fun fetchUserData() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("username") ?: "No Name"
                    val email = document.getString("email") ?: "No Email"
                    val imageUrl = document.getString("profileImage")

                    binding.txtName.text = name
                    binding.txtEmail.text = email

                    if (!imageUrl.isNullOrEmpty()) {
                        binding.imgProfile.tag = imageUrl // Save old image URL
                        Glide.with(this).load(imageUrl).into(binding.imgProfile)
                    }

                } else {
                    Toast.makeText(this, "User data not found!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Profile Picture")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> openCamera()
                1 -> openGallery()
                2 -> dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryIntent.type = "image/*"
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
    }



    companion object {
        private const val CAMERA_REQUEST_CODE = 1
        private const val GALLERY_REQUEST_CODE = 2
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            if (imageUri != null) {
                val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
                uploadImageToImgur(bitmap)
            }
        }
    }

    private var lastUploadTime: Long = 0

    private fun uploadImageToImgur(bitmap: Bitmap) {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastUploadTime < 30000) {
            Toast.makeText(this, "Please wait before uploading again!", Toast.LENGTH_SHORT).show()
            return
        }
        lastUploadTime = currentTime

        // Run compression in a background thread
        Thread {
            val resizedBitmap = resizeBitmap(bitmap, 800) // Resize to 800px width
            val byteArrayOutputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream) // 80% quality

            val requestBody = RequestBody.create(
                "image/*".toMediaTypeOrNull(),
                byteArrayOutputStream.toByteArray()
            )
            val multipartBody = MultipartBody.Part.createFormData("image", "profile.jpg", requestBody)
            val titleRequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), "Profile Picture")

            val call = RetrofitClient.instance.uploadImage(
                "Client-ID 67fa31b24986d06",
                multipartBody,
                titleRequestBody
            )

            call.enqueue(object : retrofit2.Callback<ImgurResponse> {
                override fun onResponse(call: Call<ImgurResponse>, response: retrofit2.Response<ImgurResponse>) {
                    if (response.isSuccessful) {
                        val imageUrl = response.body()?.data?.link
                        runOnUiThread {
                            showConfirmationDialog(imageUrl)
                        }

                    } else {
                        Log.e("IMGUR", "Upload failed: ${response.errorBody()?.string()}")
                        Toast.makeText(this@ProfileActivity, "Too many requests! Try again later.", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ImgurResponse>, t: Throwable) {
                    Log.e("IMGUR", "Error uploading image: ${t.message}")
                    Toast.makeText(this@ProfileActivity, "Upload error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }.start() // Run in a background thread
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val height = (maxWidth / aspectRatio).toInt()
        return Bitmap.createScaledBitmap(bitmap, maxWidth, height, false)
    }



    private fun saveImageUrlToFirestore(imageUrl: String?) {
        if (imageUrl.isNullOrEmpty()) return
        val userId = auth.currentUser?.uid ?: return
        val userRef = firestore.collection("users").document(userId)
        val realtimeDbRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

        val updates = mapOf("profileImage" to imageUrl)

        // Update Firestore
        userRef.update(updates)
            .addOnSuccessListener {
                // Update Realtime Database
                realtimeDbRef.updateChildren(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Profile picture updated in both Firestore and Realtime DB!", Toast.LENGTH_SHORT).show()
                        Glide.with(this).load(imageUrl).into(binding.imgProfile) // Load new image
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to update Realtime DB: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showConfirmationDialog(newImageUrl: String?) {
        if (newImageUrl.isNullOrEmpty()) return

        val oldImageUrl = binding.imgProfile.tag as? String // Store the previous image
        AlertDialog.Builder(this)
            .setTitle("Confirm Update")
            .setMessage("Do you want to save this new profile picture?")
            .setPositiveButton("Yes") { _, _ ->
                saveImageUrlToFirestore(newImageUrl)
            }
            .setNegativeButton("No") { _, _ ->
                Glide.with(this).load(oldImageUrl).into(binding.imgProfile) // Revert to old image
            }
            .setCancelable(false)
            .show()
    }


}
