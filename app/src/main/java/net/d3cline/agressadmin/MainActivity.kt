// MainActivity.kt
package net.d3cline.agressadmin

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var apiClient: ApiClient
    private lateinit var productsRecyclerView: RecyclerView
    private lateinit var productsAdapter: ProductsAdapter
    private val productsList = mutableListOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        apiClient = ApiClient(this)

        setContentView(R.layout.activity_main)

        productsRecyclerView = findViewById(R.id.productsRecyclerView)
        productsAdapter = ProductsAdapter(productsList)
        productsRecyclerView.adapter = productsAdapter
        productsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Set up swipe actions
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                // Not implementing drag & drop
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val product = productsList[position]
                if (direction == ItemTouchHelper.LEFT) {
                    // Delete action
                    deleteProduct(product, position)
                } else if (direction == ItemTouchHelper.RIGHT) {
                    // Edit action
                    editProduct(product)
                    productsAdapter.notifyItemChanged(position) // Reset swipe
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(productsRecyclerView)

        // Fetch products
        fetchProducts()
    }

    private fun fetchProducts() {
        // Fetch products in a background thread
        Thread {
            try {
                val products = apiClient.getProducts()
                runOnUiThread {
                    productsList.clear()
                    productsList.addAll(products)
                    productsAdapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Failed to load products: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun deleteProduct(product: Product, position: Int) {
        Thread {
            try {
                apiClient.deleteProduct(product.id)
                runOnUiThread {
                    productsList.removeAt(position)
                    productsAdapter.notifyItemRemoved(position)
                    Toast.makeText(this, "Product deleted", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    productsAdapter.notifyItemChanged(position) // Reset swipe
                    Toast.makeText(this, "Failed to delete product: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }.start()
    }

    private fun editProduct(product: Product) {
        // Start EditProductActivity with the product ID
        val intent = Intent(this, EditProductActivity::class.java)
        intent.putExtra("product_id", product.id)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                // Open SettingsActivity
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        // Refresh API client in case settings changed
        apiClient = ApiClient(this)
        fetchProducts()
    }
}
