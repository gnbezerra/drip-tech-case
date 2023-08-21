package br.com.usedrip.techcase.george.transferagent

import uk.co.jemos.podam.common.AttributeStrategy
import kotlin.random.Random

class BankCodeStrategy : AttributeStrategy<String> {
    override fun getValue(attrType: Class<*>?, attrAnnotations: MutableList<Annotation>?): String {
        return PodamStrategyUtils.getNumericString(3)
    }
}

class CPFStrategy : AttributeStrategy<String> {
    override fun getValue(attrType: Class<*>?, attrAnnotations: MutableList<Annotation>?): String {
        return PodamStrategyUtils.getNumericString(11)
    }
}

class PodamStrategyUtils private constructor() {
    companion object {
        fun getNumericString(length: Int): String {
            val sb: StringBuilder = StringBuilder(length)
            while (sb.length < length) {
                sb.append(getRandomDigit())
            }
            return sb.toString()
        }

        private fun getRandomDigit(): Char {
            return Random.nextInt(10).digitToChar()
        }
    }
}
