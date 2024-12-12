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
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import android.content.Context
import org.json.JSONObject

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
        // Set up swipe actions with visual effects
        val background = ColorDrawable()
        val deleteIcon = ContextCompat.getDrawable(this, R.drawable.ic_delete)!!
        val editIcon = ContextCompat.getDrawable(this, R.drawable.ic_edit)!!
        val intrinsicWidth = deleteIcon.intrinsicWidth
        val intrinsicHeight = deleteIcon.intrinsicHeight

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
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

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val iconMargin = (itemView.height - intrinsicHeight) / 2
                val iconTop = itemView.top + (itemView.height - intrinsicHeight) / 2
                val iconBottom = iconTop + intrinsicHeight

                if (dX > 0) {
                    // Swiping to the right - Edit
                    background.color = Color.parseColor("#4CAF50") // Green
                    background.setBounds(
                        itemView.left,
                        itemView.top,
                        itemView.left + dX.toInt(),
                        itemView.bottom
                    )
                    background.draw(c)

                    editIcon.setBounds(
                        itemView.left + iconMargin,
                        iconTop,
                        itemView.left + iconMargin + intrinsicWidth,
                        iconBottom
                    )
                    editIcon.draw(c)
                } else if (dX < 0) {
                    // Swiping to the left - Delete
                    background.color = Color.parseColor("#F44336") // Red
                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    background.draw(c)

                    deleteIcon.setBounds(
                        itemView.right - iconMargin - intrinsicWidth,
                        iconTop,
                        itemView.right - iconMargin,
                        iconBottom
                    )
                    deleteIcon.draw(c)
                } else {
                    background.setBounds(0, 0, 0, 0)
                    background.draw(c)
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(productsRecyclerView)

        // Fetch products
        fetchProducts()


        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Find the FloatingActionButton
        val fabNewProduct: com.google.android.material.floatingactionbutton.FloatingActionButton = findViewById(R.id.fabNewProduct)

        // Set an OnClickListener to open EditProductActivity in "new product" mode
        fabNewProduct.setOnClickListener {
            val intent = Intent(this, EditProductActivity::class.java)
            intent.putExtra("product_id", -1) // Using -1 to indicate a new product
            startActivity(intent)
        }



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

                    // Fetch the latest products from the server after deletion
                    fetchProducts()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    productsAdapter.notifyItemChanged(position) // Reset swipe if failed
                    Toast.makeText(this, "Failed to delete product: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }.start()
    }


    private fun editProduct(product: Product) {
        val intent = Intent(this, EditProductActivity::class.java)
        intent.putExtra("product_id", product.id) // Pass only the ID
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
