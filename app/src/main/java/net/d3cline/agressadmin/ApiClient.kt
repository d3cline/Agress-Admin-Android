// ApiClient.kt
package net.d3cline.agressadmin

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ApiClient(context: Context) {

    private val settingsManager = SettingsManager(context)
    private val baseUrl = settingsManager.baseUrl
    private val apiKey = settingsManager.apiKey

    fun getProducts(): List<Product> {
        val url = URL("$baseUrl/products")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $apiKey")
        connection.connect()

        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("Failed to get products: $responseCode")
        }

        val response = connection.inputStream.bufferedReader().use { it.readText() }

        val products = mutableListOf<Product>()
        val jsonArray = JSONArray(response)
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val product = Product(
                id = jsonObject.getInt("id"),
                name = jsonObject.getString("name"),
                price = jsonObject.getDouble("price"),
                currency = jsonObject.getString("currency"),
                description = jsonObject.getString("description"),
                image = jsonObject.getString("image"),
                created_at = jsonObject.getString("created_at"),
                updated_at = jsonObject.getString("updated_at")
            )
            products.add(product)
        }
        return products
    }

    fun getProduct(id: Int): Product {
        val url = URL("$baseUrl/product/$id")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $apiKey")
        connection.connect()

        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("Failed to get product: $responseCode")
        }

        val response = connection.inputStream.bufferedReader().use { it.readText() }

        val jsonObject = JSONObject(response)
        return Product(
            id = jsonObject.getInt("id"),
            name = jsonObject.getString("name"),
            price = jsonObject.getDouble("price"),
            currency = jsonObject.getString("currency"),
            description = jsonObject.getString("description"),
            image = jsonObject.getString("image"),
            created_at = jsonObject.getString("created_at"),
            updated_at = jsonObject.getString("updated_at")
        )
    }

    fun deleteProduct(id: Int) {
        val url = URL("$baseUrl/product/$id")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "DELETE"
        connection.setRequestProperty("Authorization", "Bearer $apiKey")
        connection.connect()

        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
            throw Exception("Failed to delete product: $responseCode")
        }
    }

    fun updateProduct(product: Product) {
        val url = URL("$baseUrl/product/${product.id}")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "PATCH"
        connection.setRequestProperty("Authorization", "Bearer $apiKey")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val productJson = JSONObject()
        productJson.put("name", product.name)
        productJson.put("price", product.price)
        productJson.put("currency", product.currency)
        productJson.put("description", product.description)
        productJson.put("image", product.image)

        val outputStream = connection.outputStream
        outputStream.write(productJson.toString().toByteArray())
        outputStream.flush()
        outputStream.close()

        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            val errorStream = connection.errorStream
            val errorResponse = errorStream.bufferedReader().use { it.readText() }
            throw Exception("Failed to update product: $responseCode $errorResponse")
        }
    }

    fun createProduct(product: Product) {
        val url = URL("$baseUrl/product")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $apiKey")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val productJson = JSONObject().apply {
            put("name", product.name)
            put("price", product.price)
            put("currency", product.currency)
            put("description", product.description)
            put("image", product.image)
        }

        val outputStream = connection.outputStream
        outputStream.write(productJson.toString().toByteArray())
        outputStream.flush()
        outputStream.close()

        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            val errorStream = connection.errorStream
            val errorResponse = errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
            throw Exception("Failed to create product: $responseCode $errorResponse")
        }
    }



    // Implement other methods like processCheckout and getOrderStatus similarly.
}
