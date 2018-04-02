package fr.manu.mapping

data class MappingContext(
        val mappingRuleResolver: ApplyStrategy = FirstMatch(), val prioritization: RulesPrioritization? = null)

// Pour rester homogène entre les différentes stratégies, on renvoie toujours une liste de rêgles (même pour le FIRST_MATCH)
sealed class ApplyStrategy {
    abstract fun resolve(rules: List<MappingLine>, element: Data): Iterable<MappingLine>
}

class FirstMatch : ApplyStrategy() {
    override fun resolve(rules: List<MappingLine>, element: Data): Iterable<MappingLine> = listOf(rules.first { it.match(element) })
}

class AllMatch : ApplyStrategy() {
    override fun resolve(rules: List<MappingLine>, element: Data): Iterable<MappingLine> = rules.filter { it.match(element) }
}