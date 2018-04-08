package fr.manu.mapping

data class MappingContext(
        val mappingRuleResolver: ApplyStrategy = FirstMatch(), val prioritization: RulesPrioritization? = null)

typealias RulesPrioritization = Comparator<MappingRule>

// Pour rester homogène entre les différentes stratégies, on renvoie toujours une liste de rêgles (même pour le FIRST_MATCH)
sealed class ApplyStrategy {
    abstract fun resolve(rules: List<MappingRule>, element: Data): Iterable<MappingRule>
}

class FirstMatch : ApplyStrategy() {
    override fun resolve(rules: List<MappingRule>, element: Data): Iterable<MappingRule> = listOf(rules.first { it.match(element) })
}

class AllMatch : ApplyStrategy() {
    override fun resolve(rules: List<MappingRule>, element: Data): Iterable<MappingRule> = rules.filter { it.match(element) }
}