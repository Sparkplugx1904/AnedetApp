package com.example.anemiadetector.data.model

/**
 * Result from anemia classification
 * @param label "Anemia" or "Non-Anemia"
 * @param confidence Score of the predicted class
 * @param allScores [score_Anemia, score_NonAnemia] - MUST expose both
 * @param isAnemic True if predicted class is Anemia (index 0)
 */
data class ClassificationResult(
    val label: String,
    val confidence: Float,
    val allScores: FloatArray,
    val isAnemic: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClassificationResult

        if (label != other.label) return false
        if (confidence != other.confidence) return false
        if (!allScores.contentEquals(other.allScores)) return false
        if (isAnemic != other.isAnemic) return false

        return true
    }

    override fun hashCode(): Int {
        var result = label.hashCode()
        result = 31 * result + confidence.hashCode()
        result = 31 * result + allScores.contentHashCode()
        result = 31 * result + isAnemic.hashCode()
        return result
    }
}
