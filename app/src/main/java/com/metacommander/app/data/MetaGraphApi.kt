package com.metacommander.app.data

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Minimal Retrofit interface. Use a server-issued short-lived user access token.
 * Never put Meta app secrets or long-lived tokens in the Android app.
 */
interface MetaGraphApi {
    @GET("me/adaccounts")
    suspend fun getAdAccounts(
        @Query("fields") fields: String = "id,name,account_status,currency,amount_spent,spend_cap",
        @Query("access_token") accessToken: String
    ): AdAccountsResponse

    companion object {
        fun create(): MetaGraphApi = Retrofit.Builder()
            .baseUrl("https://graph.facebook.com/v21.0/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build().create(MetaGraphApi::class.java)
    }
}
data class AdAccountsResponse(val data: List<MetaAdAccount> = emptyList())
data class MetaAdAccount(
    val id: String,
    val name: String,
    val account_status: Int? = null,
    val currency: String? = null,
    val amount_spent: String? = null,
    val spend_cap: String? = null
)
