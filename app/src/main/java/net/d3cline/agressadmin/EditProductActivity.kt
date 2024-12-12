package net.d3cline.agressadmin

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.yalantis.ucrop.UCrop


import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class EditProductActivity : AppCompatActivity() {

    private lateinit var settingsManager: SettingsManager
    private var productId: Int = 1

    // UI elements
    private lateinit var nameEditText: EditText
    private lateinit var priceEditText: EditText
    private lateinit var currencyEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var productImageView: ImageView
    private lateinit var pickImageButton: Button
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    private var base64Image: String = "" // For storing the image in Base64

    private lateinit var progressBar: ProgressBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        settingsManager = SettingsManager(this)
        setContentView(R.layout.activity_edit_product)

        productId = intent.getIntExtra("product_id", -1)

        if (productId == -1) {
            // New product mode - do not call loadProductDetails()
            // Fields remain blank for a new product
        } else {
            loadProductDetails() // Existing product mode
        }

        // Initialize UI elements
        nameEditText = findViewById(R.id.nameEditText)
        priceEditText = findViewById(R.id.priceEditText)
        currencyEditText = findViewById(R.id.currencyEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        productImageView = findViewById(R.id.productImageView)
        pickImageButton = findViewById(R.id.pickImageButton)
        saveButton = findViewById(R.id.saveButton)
        cancelButton = findViewById(R.id.cancelButton)

        progressBar = findViewById(R.id.progressBar)


        // Load product details
        //loadProductDetails()

        pickImageButton.setOnClickListener {
            pickImage()
        }

        saveButton.setOnClickListener {
            saveProduct()
        }

        cancelButton.setOnClickListener {
            // Go back to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun showLoadingIndicator(show: Boolean) {
        if (show) {
            progressBar.visibility = View.VISIBLE
            saveButton.isEnabled = false
            cancelButton.isEnabled = false
        } else {
            progressBar.visibility = View.GONE
            saveButton.isEnabled = true
            cancelButton.isEnabled = true
        }
    }

    private fun loadProductDetails() {
        Thread {
            try {
                val baseUrl = settingsManager.baseUrl
                val url = URL("$baseUrl/product/$productId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("Failed to fetch product: $responseCode")
                }

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val product = JSONObject(response)

                val name = product.getString("name")
                val price = product.getDouble("price").toString()
                val currency = product.getString("currency")
                val description = product.getString("description")

                val imageField = product.getString("image")
                Log.d("EditProductActivity", "Raw image field from server: $imageField")

                // If the server returns something like "data:image/webp;base64,xxxxxx"
                val parts = imageField.split(",")
                val prefix = if (parts.size > 1) parts[0] + "," else ""
                val imageBase64 = if (parts.size > 1) parts[1] else parts[0]

                Log.d("EditProductActivity", "Base64 image portion: $imageBase64")

                val imageBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                // Set base64Image so that if the user doesn't pick a new image, we still have it.
                // Reconstruct with prefix or ensure the prefix is what the server expects.
                base64Image = prefix + imageBase64

                Handler(Looper.getMainLooper()).post {
                    nameEditText.setText(name)
                    priceEditText.setText(price)
                    currencyEditText.setText(currency)
                    descriptionEditText.setText(description)
                    productImageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Failed to load product: ${e.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }.start()
    }


    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK) {
            val sourceUri: Uri = data?.data ?: return
            val destinationUri: Uri = Uri.fromFile(File(cacheDir, "cropped_${UUID.randomUUID()}.jpg"))

            UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1f, 1f)
                .start(this)
        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == Activity.RESULT_OK) {
            val croppedUri: Uri? = UCrop.getOutput(data!!)
            croppedUri?.let {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                productImageView.setImageBitmap(bitmap)
                base64Image = convertToBase64(bitmap)
            }
        }
    }

    private fun convertToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.WEBP, 90, outputStream)
        val byteArray = outputStream.toByteArray()
        return "data:image/webp;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun saveProduct() {
        val name = nameEditText.text.toString()
        val price = priceEditText.text.toString().toDoubleOrNull() ?: 0.0
        val currency = currencyEditText.text.toString()
        val description = descriptionEditText.text.toString()
        val image = base64Image

        val product = Product(
            id = productId,
            name = name,
            price = price,
            currency = currency,
            description = description,
            image = image,
            created_at = "",
            updated_at = ""
        )

        showLoadingIndicator(true)

        Thread {
            try {
                val apiClient = ApiClient(this)
                if (productId == -1) {
                    // New product mode
                    apiClient.createProduct(product)
                    runOnUiThread {
                        Toast.makeText(this, "Product created successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    // Existing product mode
                    apiClient.updateProduct(product)
                    runOnUiThread {
                        Toast.makeText(this, "Product updated successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                runOnUiThread {
                    // Hide spinner, re-enable buttons
                    showLoadingIndicator(false)
                }
            }
        }.start()
    }


    companion object {
        private const val IMAGE_PICK_CODE = 1001
    }
}
