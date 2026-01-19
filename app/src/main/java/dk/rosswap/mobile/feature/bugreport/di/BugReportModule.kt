package dk.rosswap.mobile.feature.bugreport.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dk.rosswap.mobile.feature.bugreport.data.BugReportRepositoryImpl
import dk.rosswap.mobile.feature.bugreport.domain.BugReportRepository
import javax.inject.Singleton

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class BugReportModule {

    @Binds
    @Singleton
    abstract fun bindBugReportRepository(
        impl: BugReportRepositoryImpl
    ): BugReportRepository
}
