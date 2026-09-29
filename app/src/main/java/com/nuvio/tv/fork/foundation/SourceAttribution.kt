package com.nuvio.tv.fork.foundation

/** How external code/behavior was brought in; mirrors docs/IMPORT_LEDGER.md. */
enum class ImportMode {
    REUSE,
    CHERRY_PICK,
    FILE_PORT,
    DELTA_PORT,
    ALGORITHM_PORT,
    LOGIC_PORT,
    ADAPTER,
    REWRITE,
}

/**
 * Provenance of imported behavior. docs/IMPORT_LEDGER.md stays the canonical detailed
 * record; this model only lets code refer to a pinned source when that is useful.
 */
data class SourceAttribution(
    val repository: String,
    val ref: String,
    val pinnedSha: String,
    val importMode: ImportMode,
    val sourceCommits: List<String> = emptyList(),
    val featureId: FeatureId? = null,
) {
    init {
        require(REPOSITORY.matches(repository)) { "repository must be owner/name: $repository" }
        require(ref.isNotBlank()) { "ref must not be blank" }
        require(FULL_SHA.matches(pinnedSha)) { "pinnedSha must be a full 40-char SHA: $pinnedSha" }
        sourceCommits.forEach {
            require(FULL_SHA.matches(it)) { "source commit must be a full 40-char SHA: $it" }
        }
    }

    private companion object {
        val REPOSITORY = Regex("[A-Za-z0-9._-]+/[A-Za-z0-9._-]+")
        val FULL_SHA = Regex("[0-9a-f]{40}")
    }
}
