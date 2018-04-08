package fr.manu.mapping

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.lang.Math.abs
import java.util.*

val noOpPropertiesMapping: DataConverter = { emptyMap() }

const val VALUE = "p"

class DataTestBuilder(vararg val dims: Pair<String, String>) {
    fun build(v: Double = Random().nextDouble()) = Data(dims.toMap(), mapOf(VALUE to v))
}

class MappingRuleTest {

    @Test
    fun `Application mapping avec conversion simple des montants`() {
        val sourceDimensions = mapOf(
                "ACC" to "AVANTAGES RECUS",
                "FL" to "DIMINUTION")

        val cibleDimensions = mapOf(
                "D_CA" to "C",
                "D_DP" to "01.2018",
                "D_CU" to "EUR"
        )
        val dimensionMapping = sourceDimensions to cibleDimensions

        val valuesMapping: DataConverter =
                {
                    val montant = it.values[VALUE] as Double

                    mapOf("P_AMOUNT" to montant * -1)
                }

        val rule = MappingRule(dimensionMapping, valuesMapping)

        val fact = DataTestBuilder("ACC" to "AVANTAGES RECUS", "FL" to "DIMINUTION").build(2000.0)

        assertThat(rule.match(fact)).isTrue()

        val transformedFact = rule.transform(fact)
        with(transformedFact) {
            assertThat(dimensions).isEqualTo(cibleDimensions)
            assertThat(values).isEqualTo(mapOf("P_AMOUNT" to -2000.0))
        }
    }

    @Test
    fun `Application mapping de type ecriture`() {
        val sourceDimensions = mapOf("ACC" to "AVANTAGES RECUS", "FL" to "DIMINUTION")

        val cibleDimensions = mapOf(
                "D_CA" to "C",
                "D_DP" to "01.2018",
                "D_CU" to "EUR",
                "D_DEST" to ""
        )
        val dimensionMapping = sourceDimensions to cibleDimensions

        val valuesMapping: DataConverter =
                {
                    val montant = it.values[VALUE] as Double
                    val compte = it.dimensions["ACC"]

                    val debitCredit =
                            if (compte == "AVANTAGES RECUS") {
                                if (montant < 0)
                                    abs(montant) to 0.0
                                else 0.0 to abs(montant)
                            } else .0 to .0

                    mapOf(
                            "P_LABEL" to "Lease restatement",
                            "P_DEBIT" to debitCredit.first,
                            "P_CREDIT" to debitCredit.second)
                }

        val rule = MappingRule(dimensionMapping, valuesMapping)

        val fact = DataTestBuilder("ACC" to "AVANTAGES RECUS", "FL" to "DIMINUTION").build(2000.0)

        assertThat(rule.match(fact)).isTrue()

        val transformedFact = rule.transform(fact)
        with(transformedFact) {
            assertThat(dimensions).isEqualTo(cibleDimensions)
            assertThat(values).isEqualTo(
                    mapOf(
                            "P_LABEL" to "Lease restatement",
                            "P_DEBIT" to 0.0,
                            "P_CREDIT" to 2000.0))
        }
    }

//    @Test
//    fun `Recherche des mappings à partir d'une requête`() {
//        val matchOnlyExact = MappingRule(
//                mapOf("ACC" to "AVANTAGES RECUS", "FL" to "DIMINUTION") to emptyMap(),
//                noOpPropertiesMapping)
//        val matchOnlyWildcards = MappingRule(
//                mapOf("ACC" to "*", "FL" to "*") to emptyMap()
//                , noOpPropertiesMapping)
//        val matchWithNullOrEmpty = MappingRule(
//                mapOf("ACC" to "RESERVE", "FL" to "", "LESSEE" to null) to emptyMap()
//                , noOpPropertiesMapping)
//
//        val searchByAccountValue = mapOf("ACC" to "AVANTAGES RECUS")
//        assertThat(matchOnlyExact.match(searchByAccountValue)).isTrue()
//        assertThat(matchOnlyWildcards.match(searchByAccountValue)).isTrue()
//        assertThat(matchWithNullOrEmpty.match(searchByAccountValue)).isFalse()
//
//        val searchEmptyOrNullLessee = mapOf("LESSEE" to null)
//        assertThat(matchOnlyExact.match(searchEmptyOrNullLessee)).isFalse() // Pas de LESSEE dans le mapping
//        assertThat(matchOnlyWildcards.match(searchEmptyOrNullLessee)).isFalse() // Pas de LESSEE dans le mapping
//        assertThat(matchWithNullOrEmpty.match(searchEmptyOrNullLessee)).isTrue()
//
//        val searchAllFlows = mapOf("FL" to "*")
//        assertThat(matchOnlyExact.match(searchAllFlows)).isTrue()
//        assertThat(matchOnlyWildcards.match(searchAllFlows)).isTrue()
//        assertThat(matchWithNullOrEmpty.match(searchAllFlows)).isTrue()
//
//        val searchUnknownDim = mapOf("REFERENCE" to null)
//        assertThat(matchOnlyExact.match(searchUnknownDim)).isFalse()
//        assertThat(matchOnlyWildcards.match(searchUnknownDim)).isFalse()
//        assertThat(matchWithNullOrEmpty.match(searchUnknownDim)).isFalse()
//
//        val searchUnknownDimWithValue = mapOf("REFERENCE" to "*")
//        assertThat(matchOnlyExact.match(searchUnknownDimWithValue)).isFalse()
//        assertThat(matchOnlyWildcards.match(searchUnknownDimWithValue)).isFalse()
//        assertThat(matchWithNullOrEmpty.match(searchUnknownDimWithValue)).isFalse()
//    }
}