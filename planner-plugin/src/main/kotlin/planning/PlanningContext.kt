package planning

import kotlinx.serialization.Serializable

@Serializable
data class PlanningContext(
    val intention: String,
    val specs: List<String> = emptyList(),
)
