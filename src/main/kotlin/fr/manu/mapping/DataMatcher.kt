package fr.manu.mapping

private const val ANYTHING_OR_NULL = "*"
private const val ANYTHING_NOT_NULL = "+"

fun Data.match(compareToDimensions: Map<String, String?>) =
        compareToDimensions
                .filterValues { !it.isNullOrEmpty() }
                .compareTo(this.dimensions.filterKeys { it in compareToDimensions.keys })

private fun Map<String, String?>.compareTo(other: Map<String, Any?>) =
        if (this.keys != other.keys)
            false
        else this
                .map { fr.manu.mapping.matchSingleValue(it.value, other[it.key]?.toString()) }
                .reduce { acc, b -> acc && b }


private fun matchSingleValue(sourceValue: String?, valueToCheck: String?) =
        when (sourceValue) {
            ANYTHING_OR_NULL -> true
            ANYTHING_NOT_NULL -> !valueToCheck.isNullOrEmpty()
            null -> valueToCheck.isNullOrEmpty()
            else -> convertToRegexPattern(sourceValue).matches(valueToCheck ?: "".trim())
        }

// Génère une regexp à partir des données de mapping (ça permet de gérer plus de cas que simplemement le "COMMENCE PAR")
private fun convertToRegexPattern(mappingValue: String) =
        Regex(mappingValue
                .replace(ANYTHING_OR_NULL, ".*")
                //.replace("+", ".+") // FIXME pour le moment une valeur partielle avec wildcard "+" est assimilée à une valeur exacte
                .replace("+", "\\+")
        )