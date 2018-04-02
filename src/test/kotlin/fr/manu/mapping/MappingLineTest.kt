package fr.manu.mapping

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.lang.Math.abs

val noOpPropertiesMapping: PropConverter = { emptyMap() }

const val VALUE = "p"

class DataTestBuilder(vararg val dims: Pair<String, String>) {
    fun build(v: Double) = Data(dims.toMap(), mapOf(VALUE to v))
}

class MappingLineTest {

    @Test
    fun `Correspondance des mappings (valeurs exactes)`() {
        val matchOnlyExact = mapOf(
                "ACC" to "AVANTAGES RECUS",
                "FL" to "DIMINUTION")

        val rule = MappingLine(matchOnlyExact to emptyMap(), noOpPropertiesMapping)

        val fact1 = DataTestBuilder("ACC" to "AVANTAGES RECUS", "FL" to "DIMINUTION").build(2000.0)
        val fact2 = DataTestBuilder("ACC" to "LOYER", "FL" to "DIMINUTION").build(2000.0)
        val fact3 = DataTestBuilder("ACC" to "LOYER", "FL" to "AUGMENTATION").build(2000.0)
        val fact4 = DataTestBuilder("ACC" to "AVANTAGES RECUS", "FL" to "AUGMENTATION").build(2000.0)

        assertThat(rule.match(fact1)).isTrue()
        assertThat(rule.match(fact2)).isFalse()
        assertThat(rule.match(fact3)).isFalse()
        assertThat(rule.match(fact4)).isFalse()
    }

    @Test
    fun `Correspondance des mappings (wildcards nullables)`() {
        val matchOnlyWildcards = mapOf(
                "ACC" to "*",
                "FL" to "*")

        val rule = MappingLine(matchOnlyWildcards to emptyMap(), noOpPropertiesMapping)

        val fact1 = DataTestBuilder("ACC" to "AVANTAGES RECUS", "FL" to "DIMINUTION").build(2000.0)
        val fact2 = DataTestBuilder("ACC" to "LOYER", "FL" to "DIMINUTION").build(2000.0)
        val fact3 = DataTestBuilder("ACC" to "LOYER", "FL" to "AUGMENTATION").build(2000.0)
        val fact4 = DataTestBuilder("ACC" to "AVANTAGES RECUS", "FL" to "AUGMENTATION").build(2000.0)

        assertThat(rule.match(fact1)).isTrue()
        assertThat(rule.match(fact2)).isTrue()
        assertThat(rule.match(fact3)).isTrue()
        assertThat(rule.match(fact4)).isTrue()
    }

    @Test
    fun `Correspondance des mappings (wildcards non nullables)`() {
        val matchOnlyWildcardsNonNullable = mapOf(
                "ACC" to "+",
                "FL" to "+")

        val rule = MappingLine(matchOnlyWildcardsNonNullable to emptyMap(), noOpPropertiesMapping)

        val fact1 = DataTestBuilder("ACC" to "AVANTAGES RECUS", "FL" to "DIMINUTION").build(2000.0)
        val fact2 = DataTestBuilder("ACC" to "LOYER").build(2000.0)
        val fact3 = DataTestBuilder("FL" to "AUGMENTATION").build(2000.0)
        val fact4 = DataTestBuilder("norme" to "IFRS_16").build(2000.0)

        assertThat(rule.match(fact1)).isTrue()
        assertThat(rule.match(fact2)).isFalse()
        assertThat(rule.match(fact3)).isFalse()
        assertThat(rule.match(fact4)).isFalse()
    }


    @Test
    fun `Correspondance des mappings (valeurs vide ou NULL)`() {
        val matchWithNullOrEmpty = mapOf(
                "ACC" to "*",
                "FL" to "",
                "LESSEE" to null)

        val rule = MappingLine(matchWithNullOrEmpty to emptyMap(), noOpPropertiesMapping)

        val fact1 = DataTestBuilder("ACC" to "AVANTAGES RECUS", "FL" to "DIMINUTION", "LESSEE" to "ARC").build(2000.0)
        val fact2 = DataTestBuilder("ACC" to "LOYER").build(2000.0)
        val fact3 = DataTestBuilder("FL" to "AUGMENTATION").build(2000.0)
        val fact4 = DataTestBuilder("LESSEE" to "ARC").build(2000.0)

        assertThat(rule.match(fact1)).isFalse()
        assertThat(rule.match(fact2)).isTrue()
        assertThat(rule.match(fact3)).isFalse()
        assertThat(rule.match(fact4)).isFalse()
    }

    @Test
    fun `Correspondance des mappings (mélange wildcards et valeurs exactes)`() {
        val matchMixingWildcardsAndExact = mapOf(
                "ACC" to "*",
                "FL" to "AUGMENTATION")

        val rule = MappingLine(matchMixingWildcardsAndExact to emptyMap(), noOpPropertiesMapping)

        val fact1 = DataTestBuilder("ACC" to "AVANTAGES RECUS", "FL" to "DIMINUTION").build(2000.0)
        val fact2 = DataTestBuilder("ACC" to "LOYER", "FL" to "DIMINUTION").build(2000.0)
        val fact3 = DataTestBuilder("ACC" to "LOYER", "FL" to "AUGMENTATION").build(2000.0)
        val fact4 = DataTestBuilder("ACC" to "AVANTAGES RECUS", "FL" to "AUGMENTATION").build(2000.0)

        assertThat(rule.match(fact1)).isFalse()
        assertThat(rule.match(fact2)).isFalse()
        assertThat(rule.match(fact3)).isTrue()
        assertThat(rule.match(fact4)).isTrue()
    }

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

        val valuesMapping: PropConverter =
                {
                    val montant = it.values[VALUE] as Double

                    mapOf("P_AMOUNT" to montant * -1)
                }

        val rule = MappingLine(dimensionMapping, valuesMapping)

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

        val valuesMapping: PropConverter =
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

        val rule = MappingLine(dimensionMapping, valuesMapping)

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

    @Test
    fun `Recherche des mappings à partir d'une requête`() {
        val matchOnlyExact = MappingLine(
                mapOf("ACC" to "AVANTAGES RECUS", "FL" to "DIMINUTION") to emptyMap(),
                noOpPropertiesMapping)
        val matchOnlyWildcards = MappingLine(
                mapOf("ACC" to "*", "FL" to "*") to emptyMap()
                , noOpPropertiesMapping)
        val matchWithNullOrEmpty = MappingLine(
                mapOf("ACC" to "RESERVE", "FL" to "", "LESSEE" to null) to emptyMap()
                , noOpPropertiesMapping)

        val searchByAccountValue = mapOf("ACC" to "AVANTAGES RECUS")
        assertThat(matchOnlyExact.match(searchByAccountValue)).isTrue()
        assertThat(matchOnlyWildcards.match(searchByAccountValue)).isTrue()
        assertThat(matchWithNullOrEmpty.match(searchByAccountValue)).isFalse()

        val searchEmptyOrNullLessee = mapOf("LESSEE" to null)
        assertThat(matchOnlyExact.match(searchEmptyOrNullLessee)).isFalse() // Pas de LESSEE dans le mapping
        assertThat(matchOnlyWildcards.match(searchEmptyOrNullLessee)).isFalse() // Pas de LESSEE dans le mapping
        assertThat(matchWithNullOrEmpty.match(searchEmptyOrNullLessee)).isTrue()

        val searchAllFlows = mapOf("FL" to "*")
        assertThat(matchOnlyExact.match(searchAllFlows)).isTrue()
        assertThat(matchOnlyWildcards.match(searchAllFlows)).isTrue()
        assertThat(matchWithNullOrEmpty.match(searchAllFlows)).isTrue()

        val searchUnknownDim = mapOf("REFERENCE" to null)
        assertThat(matchOnlyExact.match(searchUnknownDim)).isFalse()
        assertThat(matchOnlyWildcards.match(searchUnknownDim)).isFalse()
        assertThat(matchWithNullOrEmpty.match(searchUnknownDim)).isFalse()

        val searchUnknownDimWithValue = mapOf("REFERENCE" to "*")
        assertThat(matchOnlyExact.match(searchUnknownDimWithValue)).isFalse()
        assertThat(matchOnlyWildcards.match(searchUnknownDimWithValue)).isFalse()
        assertThat(matchWithNullOrEmpty.match(searchUnknownDimWithValue)).isFalse()
    }
}