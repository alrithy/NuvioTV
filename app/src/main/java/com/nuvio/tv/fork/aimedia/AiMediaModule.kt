package com.nuvio.tv.fork.aimedia

import android.content.Context
import com.nuvio.tv.BuildConfig
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.core.profile.ProfileScopedCredentialStore
import com.nuvio.tv.fork.aimedia.security.AndroidCipherTextStore
import com.nuvio.tv.fork.aimedia.security.AndroidInstallationIdStorage
import com.nuvio.tv.fork.aimedia.security.AndroidKeystoreBridge
import com.nuvio.tv.fork.aimedia.security.AndroidProfileGenerationStorage
import com.nuvio.tv.fork.aimedia.security.CipherTextStore
import com.nuvio.tv.fork.aimedia.security.InstallIdentity
import com.nuvio.tv.fork.aimedia.security.InstallationIdStorage
import com.nuvio.tv.fork.aimedia.security.KeystoreBridge
import com.nuvio.tv.fork.aimedia.security.ProfileGenerationStorage
import com.nuvio.tv.fork.aimedia.security.ProfileGenerationStore
import com.nuvio.tv.fork.aimedia.security.ProviderCredentialVault
import com.nuvio.tv.fork.aimedia.security.ProviderProfileCredentialStore
import com.nuvio.tv.fork.aimedia.security.ProviderTlsClientFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import java.io.File
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient

/*
 * AI media provider host wiring (G13b, D063). Adapted from Fornace/nuvio-ai @ 518af71
 * `core/di/ProviderHostModule.kt`. Every binding is a lazy singleton: nothing here is constructed
 * until the Provider Center opens, which only happens while AI media providers are opted in. The
 * one exception is profile-deletion cleanup, which resolves the vault lazily (see
 * [ProviderProfileCredentialStore]). Registry and APK downloads use the dedicated platform-TLS client,
 * never official's clients.
 */

@Module
@InstallIn(SingletonComponent::class)
abstract class AiMediaSecurityBindings {
    @Binds
    @Singleton
    abstract fun bindCipherTextStore(storage: AndroidCipherTextStore): CipherTextStore

    @Binds
    @Singleton
    abstract fun bindInstallationIdStorage(storage: AndroidInstallationIdStorage): InstallationIdStorage

    @Binds
    @Singleton
    abstract fun bindProfileGenerationStorage(storage: AndroidProfileGenerationStorage): ProfileGenerationStorage

    @Binds
    @IntoSet
    abstract fun bindProviderProfileCredentialStore(storage: ProviderProfileCredentialStore): ProfileScopedCredentialStore
}

@Module
@InstallIn(SingletonComponent::class)
object AiMediaModule {
    @Provides
    @Singleton
    fun provideKeystoreBridge(): KeystoreBridge = AndroidKeystoreBridge()

    @Provides
    @Singleton
    fun provideInstallIdentity(storage: InstallationIdStorage): InstallIdentity = InstallIdentity(storage)

    @Provides
    @Singleton
    fun provideProfileGenerationStore(storage: ProfileGenerationStorage): ProfileGenerationStore =
        ProfileGenerationStore(storage)

    @Provides
    @Singleton
    fun provideProviderCredentialVault(
        keystoreBridge: KeystoreBridge,
        cipherTextStore: CipherTextStore,
        installIdentity: InstallIdentity,
        profileGenerationStore: ProfileGenerationStore,
    ): ProviderCredentialVault = ProviderCredentialVault(
        keystoreBridge = keystoreBridge,
        cipherTextStore = cipherTextStore,
        installIdentity = installIdentity,
        profileGenerationStore = profileGenerationStore,
    )

    /** The only HTTP client AI code uses: platform trust and hostname checks, no interceptors. */
    @Provides
    @Singleton
    @AiMediaHttpClient
    fun provideAiMediaHttpClient(): OkHttpClient = ProviderTlsClientFactory().createClient()

    @Provides
    @Singleton
    fun provideProviderRegistryClient(@AiMediaHttpClient client: OkHttpClient): ProviderRegistryClient =
        ProviderRegistryClient(httpClient = client)

    @Provides
    @Singleton
    fun provideProviderArtifactVerifier(): ProviderArtifactVerifier = ProviderArtifactVerifier()

    @Provides
    @Singleton
    fun provideProviderPackageScanner(@ApplicationContext context: Context): ProviderPackageScanner =
        PackageManagerProviderPackageScanner(context.packageManager)

    @Provides
    @Singleton
    fun provideProviderContractValidator(): ProviderContractValidator = ProviderContractValidator(BuildConfig.VERSION_CODE)

    @Provides
    @Singleton
    fun provideProviderContractClient(
        @ApplicationContext context: Context,
        scanner: ProviderPackageScanner,
        validator: ProviderContractValidator,
    ): ProviderContractClient = AndroidProviderContractClient(context, scanner, validator)

    @Provides
    @Singleton
    fun provideProviderPackageOperations(@ApplicationContext context: Context): ProviderPackageOperations =
        AndroidProviderPackageOperations(context)

    @Provides
    @Singleton
    fun provideProviderApkDownloader(@AiMediaHttpClient client: OkHttpClient): ProviderApkDownloader =
        OkHttpProviderApkDownloader(client)

    @Provides
    @Singleton
    fun providePackageInstallerBridge(@ApplicationContext context: Context): PackageInstallerBridge =
        AndroidPackageInstallerBridge(context)

    @Provides
    @Singleton
    fun provideProviderInstallCoordinator(
        downloader: ProviderApkDownloader,
        verifier: ProviderArtifactVerifier,
        bridge: PackageInstallerBridge,
        @ApplicationContext context: Context,
    ): ProviderInstallCoordinator = ProviderInstallCoordinator(
        downloader = downloader,
        verifier = verifier,
        installerBridge = bridge,
        downloadDir = File(context.cacheDir, "provider-installs"),
        externalScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    )

    @Provides
    @Singleton
    fun provideVendorSelectionStore(@ApplicationContext context: Context): VendorSelectionStore =
        AndroidVendorSelectionStore(context)

    @Provides
    @Singleton
    fun provideProviderCenterController(
        registryClient: ProviderRegistryClient,
        scanner: ProviderPackageScanner,
        contractClient: ProviderContractClient,
        installCoordinator: ProviderInstallCoordinator,
        packageOperations: ProviderPackageOperations,
        vault: ProviderCredentialVault,
        profileManager: ProfileManager,
        validator: ProviderContractValidator,
        vendorSelectionStore: VendorSelectionStore,
    ): ProviderCenterController = ProviderCenterController(
        registryClient = registryClient,
        packageScanner = scanner,
        contractClient = contractClient,
        installCoordinator = installCoordinator,
        packageOperations = packageOperations,
        credentialVault = vault,
        activeProfileProvider = object : ActiveProfileProvider {
            override val activeProfileId: StateFlow<Int> = profileManager.activeProfileId
        },
        contractValidator = validator,
        vendorSelectionStore = vendorSelectionStore,
        externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    )
}

@javax.inject.Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AiMediaHttpClient
