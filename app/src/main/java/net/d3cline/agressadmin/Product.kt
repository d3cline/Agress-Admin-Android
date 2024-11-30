// Product.kt
package net.d3cline.agressadmin

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    val id: Int,
    val name: String,
    val price: Double,
    val currency: String,
    val description: String,
    val image: String,
    val created_at: String,
    val updated_at: String
) : Parcelable
