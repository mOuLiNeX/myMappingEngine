package fr.manu.mapping

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant.now
import java.time.temporal.ChronoUnit.MILLIS

private val LOGGER: Logger = LoggerFactory.getLogger(MappingEngine::class.java)

class MappingEngine(private val context: MappingContext = MappingContext(), private val rules: List<MappingRule>) {

    private val prioritizedRules get() = if (context.prioritization != null) rules.sortedWith(context.prioritization) else rules.toList()

    private val isRelevantData: (d: Data) -> Boolean = { it.dimensions.isNotEmpty() }

    fun execute(elements: Iterable<Data>): Iterable<Data> {
        val errors: MutableList<Data> = mutableListOf()

        LOGGER.debug("Application mapping sur ${elements.count()} elements(s)")

        val start = now()
        val applyMapping = { f: Data ->
            try {
                val applyingRule = context.mappingRuleResolver.resolve(prioritizedRules, f)

                applyingRule
                        .map { it.transform(f) }
                        .filter { isRelevantData(it) }
            } catch (nsee: NoSuchElementException) {
                errors.add(f)
                emptyList<Data>()
            }
        }

        val mappingResult = elements
                .filter { isRelevantData(it) }
                .map { applyMapping(it) }
                .flatten()
                // Aggregation des résultats
                .groupBy { it.dimensions }.map { it.value.reduce(Data::plus) }

        LOGGER.debug("Application mapping avec ${prioritizedRules.size} regle(s) sur ${elements.count()} element(s) pour obtenir ${mappingResult.size} nouveau(x) element(s) en ${MILLIS.between(start, now())} ms")
        return mappingResult
    }

    //fun findRuleBySource(request: Map<String, String?>) = prioritizedRules.filter { it.from() }

}

class UnmappedElementsException : Exception() {
    val elements: List<Data> = emptyList()
}
