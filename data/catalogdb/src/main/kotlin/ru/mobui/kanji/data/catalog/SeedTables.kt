package ru.mobui.kanji.data.catalog

import org.json.JSONArray
import org.json.JSONObject

internal class SeedTable(root: JSONObject, name: String, expected: List<String>) {
    private val table = root.getJSONObject("tables").getJSONObject(name)
    private val columns = table.getJSONArray("columns")

    init {
        require((0 until columns.length()).map { columns.getString(it) } == expected)
    }

    private val rows = table.getJSONArray("rows")
    private val indices = expected.withIndex().associate { it.value to it.index }

    fun <T> map(convert: (SeedRow) -> T): List<T> =
        (0 until rows.length()).map { convert(SeedRow(rows.getJSONArray(it), indices)) }
}

internal fun JSONObject.nullableString(key: String): String? =
    if (isNull(key)) null else getString(key)

internal fun JSONObject.nullableInt(key: String): Int? = if (isNull(key)) null else getInt(key)

internal class SeedRow(private val values: JSONArray, private val indices: Map<String, Int>) {
    fun getInt(column: String): Int = values.getInt(indices.getValue(column))

    fun getString(column: String): String = values.getString(indices.getValue(column))

    fun isNull(column: String): Boolean = values.isNull(indices.getValue(column))
}
