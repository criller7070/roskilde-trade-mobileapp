package dk.rosswap.mobile.feature.bugreport.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dk.rosswap.mobile.feature.bugreport.data.FirebaseBugReportRepository
import dk.rosswap.mobile.feature.bugreport.domain.BugReportRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BugReportModule {

    @Binds
    @Singleton
    abstract fun bindBugReportRepository(
        impl: FirebaseBugReportRepository
    ): BugReportRepository
}
