package net.d3cline.agressadmin

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.bumptech.glide.Glide

class EditProductActivity : AppCompatActivity() {

    private lateinit var settingsManager: SettingsManager
    private var productId: Int = 1

    // UI elements
    private lateinit var nameEditText: EditText
    private lateinit var priceEditText: EditText
    private lateinit var currencyEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var imageEditText: EditText
    private lateinit var productImageView: ImageView
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsManager = SettingsManager(this)
        setContentView(R.layout.activity_edit_product)

        productId = intent.getIntExtra("product_id", 1)

        // Initialize UI elements
        nameEditText = findViewById(R.id.nameEditText)
        priceEditText = findViewById(R.id.priceEditText)
        currencyEditText = findViewById(R.id.currencyEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        imageEditText = findViewById(R.id.imageEditText)
        productImageView = findViewById(R.id.productImageView)
        saveButton = findViewById(R.id.saveButton)
        cancelButton = findViewById(R.id.cancelButton)

        // Load product details
        loadProductDetails()

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

    private fun loadProductDetails() {
        Thread {
            try {
                // Retrieve base URL and API key
                val baseUrl = settingsManager.baseUrl

                // Fetch product details using GET request
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
                println("AYYYYYYYYYYYYYYYYYYYYYYYYYYYY")
                println(product)

// Parsing product details from JSON
                val name = product.getString("name")
                val price = product.getDouble("price").toString()
                val currency = product.getString("currency")
                val description = product.getString("description")
                val imageBase64 = product.getString("image").split(",")[1] // Strips "data:image/webp;base64," prefix

// Decode base64 image
                val imageBytes = android.util.Base64.decode(imageBase64, android.util.Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

// Update UI on the main thread
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
                    Toast.makeText(this, "Failed to load product: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                    finish()
                }
            }
        }.start()
    }

    private fun saveProduct() {
        val updatedProduct = JSONObject().apply {
            put("name", nameEditText.text.toString())
            put("price", priceEditText.text.toString().toDoubleOrNull() ?: 0.0)
            put("currency", currencyEditText.text.toString())
            put("description", descriptionEditText.text.toString())
            put("image", imageEditText.text.toString())
        }

        Thread {
            try {
                // Retrieve base URL and API key
                val baseUrl = settingsManager.baseUrl
                val apiKey = settingsManager.apiKey

                // API PATCH request to update the product
                val url = URL("$baseUrl/product/$productId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "PATCH"
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val outputStream = connection.outputStream
                outputStream.write(updatedProduct.toString().toByteArray())
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread {
                        Toast.makeText(this, "Product updated successfully", Toast.LENGTH_SHORT)
                            .show()
                        // Go back to MainActivity
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    val errorStream = connection.errorStream
                    val errorResponse =
                        errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                    throw Exception("Failed to update product: $responseCode $errorResponse")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}
