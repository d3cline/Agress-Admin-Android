package net.d3cline.agressadmin

import android.content.Intent
import android.os.Bundle
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
    private var productId: Int = 0

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

        productId = intent.getIntExtra("product_id", 0)

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
                val apiKey = settingsManager.apiKey

                // Fetch product details using GET request
                val url = URL("$baseUrl/product/$productId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                //connection.setRequestProperty("Authorization", "Bearer $apiKey")
                connection.connect()

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("Failed to fetch product: $responseCode")
                }

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val product = JSONObject(response)

                runOnUiThread {
                    // Populate the UI with product details
                    nameEditText.setText(product.getString("name"))
                    priceEditText.setText(product.getDouble("price").toString())
                    currencyEditText.setText(product.getString("currency"))
                    descriptionEditText.setText(product.getString("description"))
                    imageEditText.setText(product.getString("image"))

                    // Load image into ImageView using Glide
                    Glide.with(this)
                        .load(product.getString("image"))
                        .into(productImageView)
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
