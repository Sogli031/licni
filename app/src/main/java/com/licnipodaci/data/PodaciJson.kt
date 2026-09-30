package com.licnipodaci.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.util.UUID

object PodaciJson {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val listaTip = object : TypeToken<List<Stavka>>() {}.type
    private val uuidRegex =
        Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    fun uJson(stavke: List<Stavka>): String = gson.toJson(stavke, listaTip)

    fun izJson(json: String): Result<List<Stavka>> = runCatching {
        val root = JsonParser.parseString(json)
        val niz = root.requireArray("Koreni element mora biti niz")
        niz.mapIndexed { index, element -> element.requireStavka(index) }
            .also { stavke -> stavke.requireValidImportIds() }
    }

    private fun JsonElement.requireStavka(index: Int): Stavka {
        val objekat = requireObject("Stavka na poziciji $index mora biti objekat")
        val polja = objekat.requireArrayField("polja", "Stavka na poziciji $index mora imati niz polja")
        return Stavka(
            id = objekat.requireStringField("id", "Stavka na poziciji $index mora imati string ID"),
            kategorija = objekat.requireKategorija(index),
            naziv = objekat.requireStringField("naziv", "Stavka na poziciji $index mora imati string naziv"),
            polja = polja.mapIndexed { poljeIndex, element ->
                element.requirePolje(index, poljeIndex)
            }
        )
    }

    private fun JsonElement.requirePolje(stavkaIndex: Int, poljeIndex: Int): Polje {
        val objekat = requireObject("Polje na poziciji $stavkaIndex/$poljeIndex mora biti objekat")
        return Polje(
            oznaka = objekat.requireStringField(
                "oznaka",
                "Polje na poziciji $stavkaIndex/$poljeIndex mora imati string oznaku"
            ),
            vrednost = objekat.requireStringField(
                "vrednost",
                "Polje na poziciji $stavkaIndex/$poljeIndex mora imati string vrednost"
            )
        )
    }

    private fun JsonObject.requireKategorija(index: Int): Kategorija {
        val vrednost = requireStringField(
            "kategorija",
            "Stavka na poziciji $index mora imati string kategoriju"
        )
        return Kategorija.entries.firstOrNull { it.name == vrednost }
            ?: throw IllegalArgumentException("Stavka na poziciji $index ima nepoznatu kategoriju")
    }

    private fun List<Stavka>.requireValidImportIds() {
        val vidjeniId = mutableSetOf<String>()
        forEachIndexed { index, stavka ->
            if (!stavka.id.isValidUuid()) {
                throw IllegalArgumentException("Stavka na poziciji $index mora imati ispravan UUID ID")
            }
            if (!vidjeniId.add(stavka.id)) {
                throw IllegalArgumentException("Stavka na poziciji $index ima dupliran ID")
            }
        }
    }

    private fun String.isValidUuid(): Boolean =
        uuidRegex.matches(this) && runCatching { UUID.fromString(this) }.isSuccess

    private fun JsonObject.requireStringField(naziv: String, poruka: String): String =
        requireField(naziv, poruka).requireString(poruka)

    private fun JsonObject.requireArrayField(naziv: String, poruka: String): JsonArray =
        requireField(naziv, poruka).requireArray(poruka)

    private fun JsonObject.requireField(naziv: String, poruka: String): JsonElement =
        if (has(naziv)) {
            get(naziv)
        } else {
            throw IllegalArgumentException(poruka)
        }

    private fun JsonElement.requireObject(poruka: String): JsonObject =
        if (isJsonObject) {
            asJsonObject
        } else {
            throw IllegalArgumentException(poruka)
        }

    private fun JsonElement.requireArray(poruka: String): JsonArray =
        if (isJsonArray) {
            asJsonArray
        } else {
            throw IllegalArgumentException(poruka)
        }

    private fun JsonElement.requireString(poruka: String): String =
        if (isJsonPrimitive && asJsonPrimitive.isString) {
            asString
        } else {
            throw IllegalArgumentException(poruka)
        }
}
