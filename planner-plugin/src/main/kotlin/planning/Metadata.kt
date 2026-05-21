package planning

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File
import java.time.Instant

/**
 * Contrat de métadonnées conforme à TAXONOMIE_WORKSPACE.adoc §Format Pivot.
 * Tout borough producteur émet ce contrat à côté de sa sortie AsciiDoc.
 */
data class Metadata(
    val source: String,           // Borough producteur
    val type: String,             // Type de contenu (Plan, corpus, pipeline, quiz, etc.)
    val sessions: Int,            // Métriques de production
    val generatedAt: String,      // Horodatage ISO 8601
    val model: String,            // Modèle LLM utilisé
    val version: String,          // Version semver du contrat
    val dependencies: List<String> // Boroughs dont ce contenu dépend
) {
    companion object {
        private val mapper: ObjectMapper = ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .enable(SerializationFeature.INDENT_OUTPUT)

        /** Écrit le metadata.json dans le répertoire spécifié. */
        fun writeTo(dir: File, metadata: Metadata): File {
            dir.mkdirs()
            val file = File(dir, "metadata.json")
            file.writeText(mapper.writeValueAsString(metadata))
            return file
        }
    }
}
