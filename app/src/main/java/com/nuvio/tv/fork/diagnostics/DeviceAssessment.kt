package com.nuvio.tv.fork.diagnostics

/**
 * How a recommendation was derived (ysosrs AssessmentModels, 45e0984): MEASURED from the stream test
 * on this device, CALCULATED from queryable device facts, VERIFY when the deciding fact is not
 * queryable. VERIFY rows are advisory and never enter the apply plan.
 */
enum class AssessmentTier { MEASURED, CALCULATED, VERIFY }

enum class AssessmentKey { PARALLEL_CONNECTIONS, TARGET_BUFFER, FRAME_RATE_MATCHING }

data class AssessmentItem(val key: AssessmentKey, val tier: AssessmentTier, val changeNeeded: Boolean)

/** Settings the assessment may change; null means untouched. */
data class AssessmentPlan(
    val useParallelConnections: Boolean? = null,
    val parallelConnectionCount: Int? = null,
    val targetBufferSizeMb: Int? = null,
    val frameRateMatchingOn: Boolean? = null,
) {
    val isEmpty: Boolean
        get() = useParallelConnections == null && parallelConnectionCount == null &&
            targetBufferSizeMb == null && frameRateMatchingOn == null
}

data class AssessmentResult(val items: List<AssessmentItem>, val plan: AssessmentPlan)

data class AssessmentInputs(
    val useParallelConnections: Boolean,
    val parallelConnectionCount: Int,
    /** 0 = device default, which the official buffer engine sizes itself. */
    val targetBufferSizeMb: Int,
    val frameRateMatchingOn: Boolean,
    /** Largest target buffer that fits the safe native limit, from official MemoryBudget. */
    val maxSafeTargetBufferMb: Int,
    /** null = the display could not be inspected. */
    val displaySupportsFrameRateSwitching: Boolean?,
    /** Official stream test: best single-connection result (baseline or 1 x chunk), Mbps. */
    val singleConnectionMbps: Double?,
    /** Official stream test result at [testedConnections] parallel connections, Mbps. */
    val parallelMbps: Double?,
    val testedConnections: Int,
)

object DeviceAssessment {
    /** Parallel ranges must beat one connection by this factor to be worth their memory. */
    const val PARALLEL_GAIN_THRESHOLD = 1.15

    fun assess(input: AssessmentInputs): AssessmentResult {
        val items = mutableListOf<AssessmentItem>()
        var plan = AssessmentPlan()

        val single = input.singleConnectionMbps?.takeIf { it > 0.0 }
        val parallel = input.parallelMbps?.takeIf { it > 0.0 }
        if (single != null && parallel != null) {
            val useParallel = parallel >= single * PARALLEL_GAIN_THRESHOLD
            val change = if (useParallel) {
                !input.useParallelConnections || input.parallelConnectionCount != input.testedConnections
            } else {
                input.useParallelConnections
            }
            items += AssessmentItem(AssessmentKey.PARALLEL_CONNECTIONS, AssessmentTier.MEASURED, change)
            if (change) {
                plan = plan.copy(
                    useParallelConnections = useParallel,
                    parallelConnectionCount = if (useParallel) input.testedConnections else null,
                )
            }
        }

        val overSafe = input.targetBufferSizeMb > 0 && input.targetBufferSizeMb > input.maxSafeTargetBufferMb
        items += AssessmentItem(AssessmentKey.TARGET_BUFFER, AssessmentTier.CALCULATED, overSafe)
        if (overSafe) plan = plan.copy(targetBufferSizeMb = input.maxSafeTargetBufferMb)

        when (input.displaySupportsFrameRateSwitching) {
            null -> items += AssessmentItem(AssessmentKey.FRAME_RATE_MATCHING, AssessmentTier.VERIFY, false)
            else -> {
                val change = input.displaySupportsFrameRateSwitching != input.frameRateMatchingOn
                items += AssessmentItem(AssessmentKey.FRAME_RATE_MATCHING, AssessmentTier.CALCULATED, change)
                if (change) plan = plan.copy(frameRateMatchingOn = input.displaySupportsFrameRateSwitching)
            }
        }
        return AssessmentResult(items, plan)
    }
}
