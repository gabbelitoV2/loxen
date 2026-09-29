package com.moblin.android.various.model

import com.moblin.android.platform.log.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val TAG = "Model"

val plainIcon = Icon(name = "Plain", id = "AppIcon", price = "")

private val globalMyIcons = listOf(
    plainIcon,
    Icon(name = "San Diego", id = "AppIconSanDiego", price = "$")
)

private val iconsProductIds = listOf(
    "AppIconKing",
    "AppIconQueen",
    "AppIconLooking",
    "AppIconPixels",
    "AppIconHeart",
    "AppIconPink",
    "AppIconHappy",
    "AppIconMillionaire",
    "AppIconBillionaire",
    "AppIconTrillionaire",
    "AppIconTetris",
    "AppIconTub",
    "AppIconGoblin",
    "AppIconGoblina",
    "AppIconIreland",
    "AppIconPeru"
)

data class Icon(
    val name: String,
    val id: String,
    val price: String
) {
    fun imageNoBackground(): String = "${id}NoBackground"

    fun image(): String = id
}

suspend fun Model.getProductsFromAppStore() {
    Unit
}

fun Model.listenForAppStoreTransactions(scope: CoroutineScope): Job = scope.launch {
    Unit
}

private fun Model.checkVerified(result: Any?): Any? =
    Unit
suspend fun Model.updateProductFromAppStore() {
    Log.d(TAG, "store: Update my products from App Store")
    val myProductIds = getMyProductIds()
    updateIcons(myProductIds = myProductIds)
}

private suspend fun Model.getMyProductIds(): List<String> {
    return emptyList()
}

private fun Model.updateIcons(myProductIds: List<String>) {
    val myIcons = mutableListOf<Icon>()
    store.hasBoughtSomething = false
    val iconsInStore = mutableListOf<Icon>()
    for (productId in iconsProductIds) {
        if (!products.containsKey(productId)) {
            Log.i(TAG, "store: Icon product $productId not found")
            continue
        }
        val icon: Icon = TODO("no Android counterpart for StoreKit")
        if (myProductIds.contains(productId)) {
            myIcons.add(icon)
            store.hasBoughtSomething = true
        } else {
            iconsInStore.add(icon)
        }
    }
    store.myIcons.value = myIcons + globalMyIcons
    store.iconsInStore.value = iconsInStore
}

private fun Model.findProduct(id: String): Any? = products[id]

suspend fun Model.restorePurchases() {
    Unit
}

suspend fun Model.purchaseProduct(id: String) {
    Unit
}

private fun Model.isInMyIcons(id: String): Boolean = store.myIcons.value.any { it.id == id }

fun Model.updateIconImageFromDatabase() {
    if (!isInMyIcons(id = database.iconImage)) {
        database.iconImage = plainIcon.id
    }
    store.iconImage.value = database.iconImage
}
