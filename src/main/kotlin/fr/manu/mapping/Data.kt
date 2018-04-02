package fr.manu.mapping

val EMPTY = Data(emptyMap(), emptyMap())

private fun isNullOrEmpty(f: Data?) = (f == null || f == EMPTY)

//Trivial implementation
data class Data(val dimensions: Map<String, Any?> = emptyMap(), val values: Map<String, Any?> = emptyMap()) {

    operator fun plus(other: Data?): Data {
        if (isNullOrEmpty(other)) {
            return this
        }
        if (isNullOrEmpty(this)) {
            return other!!
        }
        if (other?.dimensions != dimensions) {
            throw UnsupportedOperationException("Cannot aggregate $other and $this (dimensions are not identical)")
        }
        return this.aggregate(other)
    }


    private fun aggregate(other: Data): Data {
        val properties = mutableMapOf<String, Any?>()

        for (key in (this.values.keys + other.values.keys)) {
            val value = this.values[key]
            val otherValue = other.values[key]

            properties[key] =
                    if (value == null && otherValue != null) {
                        otherValue
                    } else if (value != null && otherValue == null) {
                        value
                    } else if (value?.javaClass != otherValue?.javaClass) {
                        throw UnsupportedOperationException("Cannot aggregate $other and $this (values type are differents)")
                    } else when (value) {
                        is String -> if (value != otherValue) value + " " + otherValue as String else value
                        is Number -> value.toDouble() + (otherValue as Number).toDouble()
                        is Boolean -> value && otherValue as Boolean
                        else -> throw UnsupportedOperationException("Type ${value!!.javaClass} actually not handled")
                    }
        }
        return Data(this.dimensions, properties)
    }

}