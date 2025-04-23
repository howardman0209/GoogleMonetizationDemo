package com.demo.google.monetization

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.demo.google.monetization.databinding.ViewHolderProductBinding

class ProductViewHolder private constructor(private val binding: ViewHolderProductBinding) : RecyclerView.ViewHolder(binding.root) {
    companion object {
        fun from(parent: ViewGroup): ProductViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = ViewHolderProductBinding.inflate(layoutInflater, parent, false)
            return ProductViewHolder(binding)
        }
    }

    fun bind(productDetails: ProductDetails, purchase: Purchase? = null, onClicked: () -> Unit) {
        binding.tvProductName.text = productDetails.name
        binding.tvProductPrice.text = productDetails.oneTimePurchaseOfferDetails?.formattedPrice

        binding.tvPurchaseState.text = when (purchase?.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> "Purchased"
            Purchase.PurchaseState.PENDING -> "Pending"
            else -> ""
        }
        binding.root.setOnClickListener {
            onClicked.invoke()
        }
    }
}