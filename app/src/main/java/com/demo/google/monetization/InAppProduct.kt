package com.demo.google.monetization

enum class InAppProduct(val productId: String, val isConsumable: Boolean) {
    REMOVE_ADS(REMOVE_ADS_PRODUCT_ID, isConsumable = false),
    COIN_X2(COIN_X2_PRODUCT_ID, isConsumable = true)
    ;

    companion object {
        fun byProductId(productId: String): InAppProduct {
            return InAppProduct.entries.find { it.productId == productId } ?: COIN_X2
        }
    }
}