package fr.manu.mapping

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant.now
import java.time.temporal.ChronoUnit.MILLIS

private val LOGGER: Logger = LoggerFactory.getLogger(MappingEngine::class.java)

typealias RulesPrioritization = Comparator<MappingLine>

class MappingEngine(private val context: MappingContext = MappingContext(), private vararg val rules: MappingLine) {

    private val prioritizedRules get() = if (context.prioritization != null) rules.sortedWith(context.prioritization) else rules.toList()

    fun execute(elements: Iterable<Data>): Iterable<Data> {
        val errors: MutableList<Data> = mutableListOf()
        val mappingApplicationLog = StringBuilder()

        LOGGER.debug("Application mapping sur ${elements.count()} elements(s)")

        val start = now()
        val applyMapping = { f: Data ->
            try {
                context.mappingRuleResolver
                        .resolve(prioritizedRules, f)
                        .map { it.transform(f) }
            } catch (nsee: NoSuchElementException) {
                errors.add(f)
                emptyList<Data>()
            }
        }

        val transformedFacts = elements.map { applyMapping(it) }.flatten()

        val mappingResult = transformedFacts
                // Aggregation des résultats
                .filter { it.dimensions.isNotEmpty() }
                .groupBy { it.dimensions }
                .map { it.value.reduce(Data::plus) }

        LOGGER.debug("Application mapping avec ${prioritizedRules.size} regle(s) sur ${elements.count()} element(s) pour obtenir ${mappingResult.size} nouveau(x) element(s) en ${MILLIS.between(start, now())} ms")
        return mappingResult
    }

    fun findRuleBySource(request: Map<String, String?>) = prioritizedRules.filter { it.match(request) }

}

class UnmappedElementsException : Exception() {
    val elements: List<Data> = emptyList()
}
