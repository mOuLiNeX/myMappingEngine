package fr.manu.mapping

typealias PropConverter = (Data) -> Map<String, Any>

val noOpPropConverter: PropConverter = { it.values.mapValues { it.value!! } }

private const val ANYTHING_OR_NULL = "*"
private const val ANYTHING_NOT_NULL = "+"

data class MappingLine(private val dimensionsTransform: Pair<Map<String, String?>, Map<String, String>>, val valuesTransform: PropConverter) {

    val dimensionsSource get() = dimensionsTransform.first
    val dimensionsCible get() = dimensionsTransform.second

    val source
        get() = dimensionsTransform.first

    val cible
        get() = dimensionsTransform.second

    // Génère une regexp à partir des données de mapping (ça permet de gérer plus de cas que simplemement le "COMMENCE PAR")
    private fun convertToRegexPattern(mappingValue: String) =
            Regex(mappingValue
                    .replace(ANYTHING_OR_NULL, ".*")
                    //.replace("+", ".+") // FIXME pour le moment une valeur partielle avec wildcard "+" est assimilée à une valeur exacte
                    .replace("+", "\\+")
            )

    private fun matchValue(sourceValue: String?, valueToCheck: String?) =
            when (sourceValue) {
                ANYTHING_OR_NULL -> true
                ANYTHING_NOT_NULL -> !valueToCheck.isNullOrEmpty()
                null -> valueToCheck.isNullOrEmpty()
                else -> convertToRegexPattern(sourceValue).matches(valueToCheck ?: "".trim())
            }

    private fun matchRequest(sourceValue: String?, requestValue: String?) =
            when {
                sourceValue == ANYTHING_OR_NULL || requestValue == ANYTHING_OR_NULL -> true
                sourceValue == ANYTHING_NOT_NULL || requestValue == ANYTHING_NOT_NULL -> !requestValue.isNullOrEmpty() && !sourceValue.isNullOrEmpty()
                sourceValue == null && requestValue == null -> true
                else -> convertToRegexPattern(sourceValue!!).matches(requestValue ?: "".trim())
            }

    // Pour recherche d'une règle de mapping à partir d'une requête (par exp pour trouver le compte de charges d'intérêt)
    fun match(request: Map<String, String?>): Boolean {
        val searchableDims = dimensionsSource.filterKeys { it in request.keys }

        if (searchableDims.isEmpty()) {
            return false
        }

        val entry = searchableDims.entries.iterator()

        var match = true
        while (entry.hasNext() && match) {
            val source = entry.next()

            val sourceValue = source.value
            val requestValue = request[source.key]
            match = matchRequest(sourceValue, requestValue)
        }
        return match
    }

    // Correspondance entre un element et une règle de mapping
    fun match(element: Data): Boolean {
        val sourceMappingDimensions = dimensionsSource
        val entry = dimensionsSource.entries.iterator()

        var match = true
        while (entry.hasNext() && match) {
            val source = entry.next()

            match = matchValue(source.value, element.dimensions[source.key]?.toString())
        }
        return match

    }

    fun transform(fact: Data): Data = Data(dimensionsCible, valuesTransform(fact))
}

fun mapAnything(value: String) = (value == ANYTHING_OR_NULL) || (value == ANYTHING_NOT_NULL)