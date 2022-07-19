package com.example.mywallet.feature_wallet.data.local.entity

import androidx.room.ProvidedTypeConverter
import com.example.mywallet.feature_wallet.data.util.JsonParser

@ProvidedTypeConverter  //Tell Room we need to Provide our own convertor since Convertors are not allowed to have constructors hence we instantiate ourselves
class Converters(
    private val jsonParser: JsonParser
) {

/** Example
//    @TypeConverter
//    fun fromMeaningsJson(json: String): List<Meaning> {
//        return jsonParser.fromJson<ArrayList<Meaning>>(
//            json,
//            object : TypeToken<ArrayList<Meaning>>() {}.type
//        ) ?: emptyList()
//    }
//
//    @TypeConverter
//    fun toMeaningsJson(meanings: List<Meaning>): String {
//        return jsonParser.toJson(
//            meanings,
//            object : TypeToken<ArrayList<Meaning>>() {}.type
//        ) ?: "[]"
//    }
**/
}