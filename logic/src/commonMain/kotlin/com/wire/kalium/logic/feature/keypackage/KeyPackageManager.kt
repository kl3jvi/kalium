package com.wire.kalium.logic.feature.keypackage

import com.wire.kalium.logic.data.sync.IncrementalSyncRepository
import com.wire.kalium.logic.data.sync.IncrementalSyncStatus
import com.wire.kalium.logic.feature.TimestampKeyRepository
import com.wire.kalium.logic.feature.TimestampKeys.LAST_KEY_PACKAGE_COUNT_CHECK
import com.wire.kalium.logic.featureFlags.FeatureSupport
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.flatMap
import com.wire.kalium.logic.functional.onFailure
import com.wire.kalium.logic.kaliumLogger
import com.wire.kalium.util.KaliumDispatcher
import com.wire.kalium.util.KaliumDispatcherImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours

// The duration in hours after which we should re-check key package count.
internal val KEY_PACKAGE_COUNT_CHECK_DURATION = 24.hours

/**
 * Observes the MLS key package count and uploads new key packages when necessary.
 */
internal interface KeyPackageManager

internal class KeyPackageManagerImpl(
    private val featureSupport: FeatureSupport,
    private val incrementalSyncRepository: IncrementalSyncRepository,
    private val refillKeyPackagesUseCase: Lazy<RefillKeyPackagesUseCase>,
    private val keyPackageCountUseCase: Lazy<MLSKeyPackageCountUseCase>,
    private val timestampKeyRepository: Lazy<TimestampKeyRepository>,
    kaliumDispatcher: KaliumDispatcher = KaliumDispatcherImpl
) : KeyPackageManager {
    /**
     * A dispatcher with limited parallelism of 1.
     * This means using this dispatcher only a single coroutine will be processed at a time.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val dispatcher = kaliumDispatcher.default.limitedParallelism(1)

    private val refillKeyPackagesScope = CoroutineScope(dispatcher)

    private var refillKeyPackageJob: Job? = null

    init {
        refillKeyPackageJob = refillKeyPackagesScope.launch {
            incrementalSyncRepository.incrementalSyncState.collect { syncState ->
                ensureActive()
                if (syncState is IncrementalSyncStatus.Live && featureSupport.isMLSSupported) {
                    refillKeyPackagesIfNeeded()
                }
            }
        }
    }

    private suspend fun refillKeyPackagesIfNeeded() {
        timestampKeyRepository.value.hasPassed(LAST_KEY_PACKAGE_COUNT_CHECK, KEY_PACKAGE_COUNT_CHECK_DURATION)
            .flatMap { lastKeyPackageCountCheckHasPassed ->
                val forceRefill = keyPackageCountUseCase.value(fromAPI = false).let {
                    when (it) {
                        is MLSKeyPackageCountResult.Success -> it.needsRefill
                        // if that fails, during the next sync we will check again! so it doesn't matter to skip one time!
                        else -> false
                    }
                }
                if (lastKeyPackageCountCheckHasPassed || forceRefill) {
                    kaliumLogger.i("Checking if we need to refill key packages")
                    when (val result = refillKeyPackagesUseCase.value()) {
                        is RefillKeyPackagesResult.Success -> timestampKeyRepository.value.reset(LAST_KEY_PACKAGE_COUNT_CHECK)
                        is RefillKeyPackagesResult.Failure -> Either.Left(result.failure)
                    }
                }
                Either.Right(Unit)
            }.onFailure { kaliumLogger.w("Error while refilling key packages: $it") }
    }

}