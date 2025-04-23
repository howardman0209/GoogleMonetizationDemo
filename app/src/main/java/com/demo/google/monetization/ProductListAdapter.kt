package com.demo.google.monetization

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase

class ProductListAdapter(private val listener: ProductViewListener) : RecyclerView.Adapter<ProductViewHolder>() {
    val products = mutableListOf<ProductDetails>()
    val purchases = mutableListOf<Purchase>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        return ProductViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val productDetails = products[position]
        val correspondingPurchase = purchases.find { it.products.contains(productDetails.productId) }
        holder.bind(productDetails, correspondingPurchase) {
            listener.onProductSelected(productDetails)
        }
    }

    override fun getItemCount(): Int = products.size

    @SuppressLint("NotifyDataSetChanged")
    fun setProducts(data: List<ProductDetails>) {
        Log.d("ProductListAdapter, setProducts", "products: $products")
        products.clear()
        products.addAll(data)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setPurchases(data: List<Purchase>) {
        Log.d("ProductListAdapter, setPurchases", "purchases: $purchases")
        purchases.clear()
        purchases.addAll(data)
        notifyDataSetChanged()
    }

    interface ProductViewListener {
        fun onProductSelected(productDetails: ProductDetails)
    }
}