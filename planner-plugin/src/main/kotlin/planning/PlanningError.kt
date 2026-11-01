package planning

/**
 * Typed planning failures — errors as values (XB-FP-4, arrow-kt).
 *
 * The planner used to keep the last exception in a mutable variable inside a
 * `for` loop and throw an `IllegalStateException` at the end. Failures are now
 * values carried by `Either<PlanningError, Plan>`; the exception is rebuilt
 * exactly once, at the public boundary.
 */
sealed interface PlanningError {
    /** Which attempt produced the failure (1-based). */
    val attempt: Int

    /** Human-readable detail (exception message when available). */
    val detail: String?

    /** Underlying cause, if any. */
    val cause: Throwable?

    /** The LLM call itself failed (transport, model error, …). */
    data class LlmFailure(
        override val attempt: Int,
        override val detail: String?,
        override val cause: Throwable? = null,
    ) : PlanningError

    /** The LLM answered but the response could not be parsed into a [Plan]. */
    data class ParseFailure(
        override val attempt: Int,
        override val detail: String?,
        override val cause: Throwable? = null,
    ) : PlanningError
}
