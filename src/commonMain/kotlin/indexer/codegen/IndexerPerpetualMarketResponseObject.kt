/**
 * Indexer API
 * No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 * OpenAPI spec version: v1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */
package indexer.codegen

import kotlinx.serialization.Serializable

/**
 *
 * @param clobPairId
 * @param ticker
 * @param status
 * @param oraclePrice
 * @param priceChange24H
 * @param volume24H
 * @param trades24H
 * @param nextFundingRate
 * @param initialMarginFraction
 * @param maintenanceMarginFraction
 * @param openInterest
 * @param atomicResolution
 * @param quantumConversionExponent
 * @param tickSize
 * @param stepSize
 * @param stepBaseQuantums
 * @param subticksPerTick
 * @param marketType
 * @param openInterestLowerCap
 * @param openInterestUpperCap
 * @param baseOpenInterest
 */
@Serializable
data class IndexerPerpetualMarketResponseObject(

    val clobPairId: kotlin.String,
    val ticker: kotlin.String,
    val status: IndexerPerpetualMarketStatus,
    val oraclePrice: kotlin.String,
    val priceChange24H: kotlin.String,
    val volume24H: kotlin.String,
    val trades24H: kotlin.Int,
    val nextFundingRate: kotlin.String,
    val initialMarginFraction: kotlin.String,
    val maintenanceMarginFraction: kotlin.String,
    val openInterest: kotlin.String,
    val atomicResolution: kotlin.Int,
    val quantumConversionExponent: kotlin.Int,
    val tickSize: kotlin.String,
    val stepSize: kotlin.String,
    val stepBaseQuantums: kotlin.Int,
    val subticksPerTick: kotlin.Int,
    val marketType: IndexerPerpetualMarketType,
    val openInterestLowerCap: kotlin.String? = null,
    val openInterestUpperCap: kotlin.String? = null,
    val baseOpenInterest: kotlin.String
)
