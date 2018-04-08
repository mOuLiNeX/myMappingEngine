package fr.manu.mapping

import org.assertj.core.api.Assertions
import org.junit.Test

class DataMatcherTest {
    @Test
    fun `Correspondance des mappings (valeurs exactes)`() {
        val exactMatching = mapOf(
                "Dim1" to "A",
                "Dim2" to "1")

        val data1 = DataTestBuilder("Dim1" to "A", "Dim2" to "1").build()
        val data2 = DataTestBuilder("Dim1" to "B", "Dim2" to "1").build()
        val data3 = DataTestBuilder("Dim1" to "B", "Dim2" to "2").build()
        val data4 = DataTestBuilder("Dim1" to "A", "Dim2" to "2").build()

        Assertions.assertThat(data1.match(exactMatching)).isTrue()
        Assertions.assertThat(data2.match(exactMatching)).isFalse()
        Assertions.assertThat(data3.match(exactMatching)).isFalse()
        Assertions.assertThat(data4.match(exactMatching)).isFalse()
    }

    @Test
    fun `Correspondance des mappings (wildcards nullables)`() {
        val wildcardMatching = mapOf(
                "Dim1" to "*",
                "Dim2" to "*")

        val data1 = DataTestBuilder("Dim1" to "A", "Dim2" to "1").build()
        val data2 = DataTestBuilder("Dim1" to "B", "Dim2" to "1").build()
        val data3 = DataTestBuilder("Dim1" to "B", "Dim2" to "2").build()
        val data4 = DataTestBuilder("Dim1" to "A", "Dim2" to "2").build()

        Assertions.assertThat(data1.match(wildcardMatching)).isTrue()
        Assertions.assertThat(data2.match(wildcardMatching)).isTrue()
        Assertions.assertThat(data3.match(wildcardMatching)).isTrue()
        Assertions.assertThat(data4.match(wildcardMatching)).isTrue()
    }

    @Test
    fun `Correspondance des mappings (wildcards non nullables)`() {
        val wildcardsNonNullableMatching = mapOf(
                "Dim1" to "+",
                "Dim2" to "+")

        val data1 = DataTestBuilder("Dim1" to "A", "Dim2" to "1").build()
        val data2 = DataTestBuilder("Dim1" to "B").build()
        val data3 = DataTestBuilder("Dim2" to "C").build()
        val data4 = DataTestBuilder("Dim42" to "BALEC").build()

        Assertions.assertThat(data1.match(wildcardsNonNullableMatching)).isTrue()
        Assertions.assertThat(data2.match(wildcardsNonNullableMatching)).isFalse()
        Assertions.assertThat(data3.match(wildcardsNonNullableMatching)).isFalse()
        Assertions.assertThat(data4.match(wildcardsNonNullableMatching)).isFalse()
    }


    @Test
    fun `Correspondance des mappings (valeurs vide ou NULL)`() {
        val nullOrEmpty = mapOf(
                "Dim1" to "*",
                "Dim2" to "",
                "Dim3" to null)

        val data1 = DataTestBuilder("Dim1" to "A", "Dim2" to "1", "Dim3" to "Z").build()
        val data2 = DataTestBuilder("Dim1" to "B").build()
        val data3 = DataTestBuilder("Dim2" to "C").build()
        val data4 = DataTestBuilder("Dim3" to "BALEC").build()

        Assertions.assertThat(data1.match(nullOrEmpty)).isFalse()
        Assertions.assertThat(data2.match(nullOrEmpty)).isTrue()
        Assertions.assertThat(data3.match(nullOrEmpty)).isFalse()
        Assertions.assertThat(data4.match(nullOrEmpty)).isFalse()
    }

    @Test
    fun `Correspondance des mappings (mélange wildcards et valeurs exactes)`() {
        val mixWildcardsAndExactValues = mapOf(
                "Dim1" to "*",
                "Dim2" to "2")

        val data1 = DataTestBuilder("Dim1" to "A", "Dim2" to "1").build()
        val data2 = DataTestBuilder("Dim1" to "B", "Dim2" to "1").build()
        val data3 = DataTestBuilder("Dim1" to "B", "Dim2" to "2").build()
        val data4 = DataTestBuilder("Dim1" to "A", "Dim2" to "2").build()

        Assertions.assertThat(data1.match(mixWildcardsAndExactValues)).isFalse()
        Assertions.assertThat(data2.match(mixWildcardsAndExactValues)).isFalse()
        Assertions.assertThat(data3.match(mixWildcardsAndExactValues)).isTrue()
        Assertions.assertThat(data4.match(mixWildcardsAndExactValues)).isTrue()
    }
}