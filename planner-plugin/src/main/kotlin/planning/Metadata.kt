package planning

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File

/**
 * Contrat de métadonnées conforme à TAXONOMIE_WORKSPACE.adoc §Format Pivot.
 * Tout borough producteur émet ce contrat à côté de sa sortie AsciiDoc.
 */
data class Metadata(
    // Borough producteur
    val source: String,
    // Type de contenu (Plan, corpus, pipeline, quiz, etc.)
    val type: String,
    // Métriques de production
    val sessions: Int,
    // Horodatage ISO 8601
    val generatedAt: String,
    // Modèle LLM utilisé
    val model: String,
    // Version semver du contrat
    val version: String,
    // Boroughs dont ce contenu dépend
    val dependencies: List<String>,
) {
    companion object {
        private val mapper: ObjectMapper =
            ObjectMapper()
                .registerModule(KotlinModule.Builder().build())
                .enable(SerializationFeature.INDENT_OUTPUT)

        /** Écrit le metadata.json dans le répertoire spécifié. */
        fun writeTo(
            dir: File,
            metadata: Metadata,
        ): File {
            dir.mkdirs()
            val file = File(dir, "metadata.json")
            file.writeText(mapper.writeValueAsString(metadata))
            return file
        }
    }
}
