package fr.manu.mapping

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Ignore
import org.junit.Test


class MappingEngineTest {
    private fun factorFunction(factor: Int): PropConverter = {
        val montant = it.values[VALUE] as Double
        mapOf("mappingTarget" to montant * factor)
    }

    @Test
    fun `Application mapping pour obtenir des éléments transformés sans aggrégation`() {
        // GIVEN
        val rule1 = MappingLine(
                mapOf("ACC" to "AVANTAGES RECUS", "FL" to "DIMINUTION")
                        to mapOf("D_CA" to "C", "D_DP" to "01.2018", "D_CU" to "EUR", "D_FL" to "F00"),
                factorFunction(1))
        val rule2 = MappingLine(
                mapOf("ACC" to "RESERVE", "FL" to "DIMINUTION")
                        to mapOf("D_CA" to "C", "D_DP" to "01.2018", "D_CU" to "EUR", "D_FL" to "F99"),
                factorFunction(-1))

        val facts = listOf(
                DataTestBuilder("ACC" to "AVANTAGES RECUS", "FL" to "DIMINUTION").build(2000.0),
                DataTestBuilder("ACC" to "RESERVE", "FL" to "DIMINUTION").build(1000.0))

        // WHEN
        val mapping = MappingEngine(rules = *arrayOf(rule1, rule2))
        val transformedDatas = mapping.execute(facts)

        // THEN
        assertThat(transformedDatas).containsExactly(
                Data(mapOf("D_CA" to "C", "D_DP" to "01.2018", "D_CU" to "EUR", "D_FL" to "F00"), mapOf("mappingTarget" to 2000.0)),
                Data(mapOf("D_CA" to "C", "D_DP" to "01.2018", "D_CU" to "EUR", "D_FL" to "F99"), mapOf("mappingTarget" to -1000.0))
        )
    }

    @Test
    fun `Application mapping pour obtenir des éléments aggrégés sur une seule et même destination`() {
        // GIVEN
        val singleDestination = mapOf("D_CA" to "C", "D_DP" to "01.2018", "D_CU" to "EUR", "D_FL" to "F00")
        val rule1 = MappingLine(
                mapOf("ACC" to "AVANTAGES RECUS", "FL" to "DIMINUTION")
                        to singleDestination,
                factorFunction(1))
        val rule2 = MappingLine(
                mapOf("ACC" to "RESERVE", "FL" to "DIMINUTION")
                        to singleDestination,
                factorFunction(-1))

        val facts = listOf(
                DataTestBuilder("ACC" to "AVANTAGES RECUS", "FL" to "DIMINUTION").build(2000.0),
                DataTestBuilder("ACC" to "RESERVE", "FL" to "DIMINUTION").build(1000.0))

        // WHEN
        val mapping = MappingEngine(rules = *arrayOf(rule1, rule2))
        val transformedDatas = mapping.execute(facts)

        // THEN
        assertThat(transformedDatas).containsExactly(
                Data(singleDestination, mapOf("mappingTarget" to (2000.0 - 1000.0)))
        )
    }

    @Test
    fun `Prise en compte des règles de priorité`() {
        // GIVEN
        val ruleExact = MappingLine(mapOf("ACC" to "AVANTAGES RECUS") to mapOf("sortie" to "C'était un avantage"), noOpPropertiesMapping)
        val ruleWildcard = MappingLine(mapOf("ACC" to "*") to mapOf("sortie" to "C'était n'importe quoi"), noOpPropertiesMapping)

        val facts = listOf(
                DataTestBuilder("ACC" to "AVANTAGES RECUS").build(2000.0),
                DataTestBuilder("ACC" to "RESERVE").build(1000.0))

        val wildcardsFirst = RulesPrioritization { m1: MappingLine, m2: MappingLine ->
            if (m1.dimensionsSource["ACC"] == "*") -1 else 1
        }
        val wildcardsLast = RulesPrioritization { m1: MappingLine, m2: MappingLine ->
            if (m1.dimensionsSource["ACC"] == "*") 1 else -1
        }

        // WHEN
        val mappingWildcardsFirst = MappingEngine(MappingContext(prioritization = wildcardsFirst), ruleExact, ruleWildcard)
        val mappingWildcardsLast = MappingEngine(MappingContext(prioritization = wildcardsLast), ruleExact, ruleWildcard)

        // THEN
        // Si on termine par les wildcards alors on passe les règles spécifiques d'abord et on a un fallback
        assertThat(mappingWildcardsLast.execute(facts)).containsExactly(
                Data(mapOf("sortie" to "C'était un avantage"), emptyMap()),
                Data(mapOf("sortie" to "C'était n'importe quoi"), emptyMap()))

        // Si on commence par les wildcards alors on produira toujours la même cibleqque soit les données en entrée
        assertThat(mappingWildcardsFirst.execute(facts)).containsExactly(
                Data(mapOf("sortie" to "C'était n'importe quoi"), emptyMap()))
    }


    @Test
    fun `Prise en compte de la stratégie de recherche des rêgles de mapping`() {
        // GIVEN
        val ruleExact = MappingLine(mapOf("ACC" to "AVANTAGES RECUS") to mapOf("sortie" to "C'était un avantage"), noOpPropertiesMapping)
        val ruleWildcard = MappingLine(mapOf("ACC" to "*") to mapOf("sortie" to "C'était n'importe quoi"), noOpPropertiesMapping)

        val facts = listOf(DataTestBuilder("ACC" to "AVANTAGES RECUS").build(2000.0))

        // Par sécurité et pour ne pas avoir d'ambiguïté sur la recherche des mappings on fixe un ordre
        val wildcardsLast = RulesPrioritization { m1: MappingLine, m2: MappingLine ->
            if (m1.dimensionsSource["ACC"] == "*") 1 else -1
        }

        // WHEN
        val mappingOneRuleApplicable = MappingEngine(MappingContext(prioritization = wildcardsLast, mappingRuleResolver = FirstMatch()), ruleExact, ruleWildcard)
        val mappingAllRulesApplicable = MappingEngine(MappingContext(prioritization = wildcardsLast, mappingRuleResolver = AllMatch()), ruleExact, ruleWildcard)

        // THEN
        // Si on ne s'arrête qu'à la 1ère rêgle de mapping applicable alors on n'a qu'une seule transformation
        assertThat(mappingOneRuleApplicable.execute(facts)).containsExactly(
                Data(mapOf("sortie" to "C'était un avantage"), emptyMap()))

        // Si on prend ttes les rêgles de mapping applicables alors on a 2 transformations
        assertThat(mappingAllRulesApplicable.execute(facts)).containsExactly(
                Data(mapOf("sortie" to "C'était un avantage"), emptyMap()),
                Data(mapOf("sortie" to "C'était n'importe quoi"), emptyMap()))


    }

    @Ignore
    @Test
    fun `Si le mapping n'est pas exhaustif, alors on doit remonter avec la liste des éléments non mappés`() {
        // GIVEN
        val factor: Int = -1
        val rule = MappingLine(
                mapOf("ACC" to "RESERVE", "FL" to "DIMINUTION")
                        to mapOf("D_CA" to "C", "D_DP" to "01.2018", "D_CU" to "EUR", "D_FL" to "F99"),
                factorFunction(-1))


        val expectedNonMappableDatas = listOf(
                DataTestBuilder("ACC" to "AVANTAGES RECUS", "FL" to "DIMINUTION").build(2000.0),
                DataTestBuilder("ACC" to "RESERVE", "FL" to "AUGMENTATION").build(1000.0))

        val factToBeMapped = DataTestBuilder("ACC" to "RESERVE", "FL" to "DIMINUTION").build(1000.0)

        val mapping = MappingEngine(rules = rule)

        // WHEN
        try {
            mapping.execute(expectedNonMappableDatas + factToBeMapped)
            fail("U should not be here")
        } catch (uee: UnmappedElementsException) {
            //THEN
            assertThat(uee.elements).containsExactlyElementsOf(expectedNonMappableDatas)
        }
    }

    @Test
    fun `Recherche de règle de mapping`() {
        val rule1 = MappingLine(
                mapOf("ACC" to "AVANTAGES RECUS", "FL" to "DIMINUTION")
                        to mapOf("D_CA" to "C", "D_DP" to "01.2018", "D_CU" to "EUR", "D_FL" to "F00"),
                factorFunction(1))
        val rule2 = MappingLine(
                mapOf("ACC" to "RESERVE", "FL" to "DIMINUTION")
                        to mapOf("D_CA" to "C", "D_DP" to "01.2018", "D_CU" to "EUR", "D_FL" to "F99"),
                factorFunction(-1))

        val mapping = MappingEngine(rules = *arrayOf(rule1, rule2))

        val avantagesAccountMapping = mapping.findRuleBySource(mapOf("ACC" to "AVANTAGES RECUS"))
        assertThat(avantagesAccountMapping).containsExactly(rule1)

        val diminutionFlowMapping = mapping.findRuleBySource(mapOf("FL" to "DIMINUTION"))
        assertThat(diminutionFlowMapping).containsExactly(rule1, rule2)

        val allAccountMapping = mapping.findRuleBySource(mapOf("ACC" to "*"))
        assertThat(allAccountMapping).containsExactly(rule1, rule2)

        val noAccountMapping = mapping.findRuleBySource(mapOf("ACC" to "LOYER"))
        assertThat(noAccountMapping).isEmpty()

        val noMapping = mapping.findRuleBySource(mapOf("LESSEE" to "*"))
        assertThat(noMapping).isEmpty()
    }

}