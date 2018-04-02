package fr.manu.mapping

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import java.time.Instant

class DataTest {

    @Test
    fun `Somme sur vide`() {
        assertThat(EMPTY + EMPTY).isEqualTo(EMPTY)
    }

    @Test
    fun `Null = element neutre de la somme`() {
        val f1 = Data(mapOf("DIM1" to "1"), mapOf("VAL1" to "A"))
        assertThat(f1 + null).isEqualTo(f1)
    }

    @Test
    fun `EMPTY = element neutre de la somme`() {
        val f1 = Data(mapOf("DIM1" to "1"), mapOf("VAL1" to "A"))
        assertThat(f1 + EMPTY).isEqualTo(f1)
        assertThat(EMPTY + f1).isEqualTo(f1)
        assertThat(EMPTY + EMPTY).isEqualTo(EMPTY)
    }

    @Test
    fun `On ne peut pas sommer s'ils n'ont pas les mêmes dimensions`() {
        assertThatThrownBy {
            Data(mapOf("DIM1" to "Val1")) + Data(mapOf("DIM2" to "Val2"))
        }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy {
            Data(mapOf("DIM1" to "Val1")) + Data(mapOf("DIM1" to "Val2"))
        }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy {
            Data(mapOf("DIM1" to "Val1")) + Data(mapOf("DIM2" to "Val1"))
        }.isInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    fun `Somme avec union de valeurs`() {
        assertThat(Data(values = mapOf("Prop1" to 3)) + Data(values = mapOf("Prop2" to 4)))
                .isEqualTo(Data(values = mapOf("Prop1" to 3, "Prop2" to 4)))
    }

    @Test
    fun `Somme avec types incompatibles (str vs numerique)`() {
        assertThatThrownBy {
            Data(values = mapOf("Prop" to "3")) + Data(values = mapOf("Prop" to 4))
        }.isInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    fun `Somme avec des numériques`() {
        assertThat(Data(values = mapOf("Prop" to 3.0)) + Data(values = mapOf("Prop" to 4.0)))
                .isEqualTo(Data(values = mapOf("Prop" to 7.0)))
    }

    @Test
    fun `Somme avec du texte`() {
        assertThat(Data(values = mapOf("Prop" to "Ceci est")) + Data(values = mapOf("Prop" to "un texte")))
                .isEqualTo(Data(values = mapOf("Prop" to "Ceci est un texte"))) // Notez bien qu'on a rajouté un espace
    }

    @Test
    fun `Cas particulier de la somme avec du texte identique`() {
        assertThat(Data(values = mapOf("Prop" to "Ceci est un texte")) + Data(values = mapOf("Prop" to "Ceci est un texte")))
                .isEqualTo(Data(values = mapOf("Prop" to "Ceci est un texte")))
    }

    @Test
    fun `Somme avec du booléen`() {
        assertThat(Data(values = mapOf("Prop" to true)) + Data(values = mapOf("Prop" to false)))
                .isEqualTo(Data(values = mapOf("Prop" to (true && false)))) // par convention on fait un ET LOGIQUE
    }

    @Test
    fun `Somme avec des dates (pas encore implémenté)`() {
        assertThatThrownBy {
            Data(values = mapOf("Prop" to Instant.now())) + Data(values = mapOf("Prop" to Instant.now()))
        }.isInstanceOf(UnsupportedOperationException::class.java)
    }
}