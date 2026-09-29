package com.nuvio.tv.fork.foundation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SourceAttributionTest {

    private val sha = "fd7973d91dd75d790c5f9b3d68dae652655e92c4"

    @Test
    fun describesPinnedSource() {
        val attribution = SourceAttribution(
            repository = "ysosrs123/NuvioTV-Fork",
            ref = "nuvio-test",
            pinnedSha = "45e0984c18460d2a65c5d745999011b4314328eb",
            importMode = ImportMode.DELTA_PORT,
            sourceCommits = listOf(sha),
            featureId = FeatureId.UNIFIED_DIAGNOSTICS,
        )
        assertEquals(ImportMode.DELTA_PORT, attribution.importMode)
        assertEquals(listOf(sha), attribution.sourceCommits)
        assertEquals(FeatureId.UNIFIED_DIAGNOSTICS, attribution.featureId)
    }

    @Test
    fun rejectsBranchNameOrShortShaAsPin() {
        assertThrows(IllegalArgumentException::class.java) {
            SourceAttribution("NuvioMedia/NuvioTV", "dev", "dev", ImportMode.REUSE)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SourceAttribution("NuvioMedia/NuvioTV", "dev", sha.take(7), ImportMode.REUSE)
        }
    }

    @Test
    fun rejectsInvalidSourceCommit() {
        assertThrows(IllegalArgumentException::class.java) {
            SourceAttribution("NuvioMedia/NuvioTV", "dev", sha, ImportMode.CHERRY_PICK, listOf("abc123"))
        }
    }

    @Test
    fun rejectsMalformedRepositoryOrBlankRef() {
        assertThrows(IllegalArgumentException::class.java) {
            SourceAttribution("NuvioTV", "dev", sha, ImportMode.REUSE)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SourceAttribution("NuvioMedia/NuvioTV", " ", sha, ImportMode.REUSE)
        }
    }
}
