/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.backend.classic.ClassicJvmBackendFacade
import org.jetbrains.kotlin.test.backend.handlers.JvmBoxRunner
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.testRunner
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2ClassicBackendConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.classic.handlers.DeclarationsDumpHandler
import org.jetbrains.kotlin.test.frontend.classic.handlers.OldNewInferenceMetaInfoProcessor
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDumpHandler
import org.jetbrains.kotlin.test.model.BackendKind
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKind
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.junit.jupiter.api.Test
import java.io.File

@TestMetadata("compiler/new-tests-infrastructure/testData")
@TestDataPath("\$PROJECT_ROOT")
class SomeTest {
    private val firAnalysisConfiguration: TestConfigurationBuilder.() -> Unit = {
        globalDefaults {
            frontend = FrontendKind.FIR
            backend = BackendKind.NoBackend
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Source
        }

        assertions = JUnit5Assertions

        useConfigurators(::JvmEnvironmentConfigurator)

        useFrontendFacades(::FirFrontendFacade)
        useFrontendHandlers(::FirDumpHandler, ::FirDiagnosticsHandler)
    }

    @Test
    @TestMetadata("a.kt")
    fun testFirAnalysisA() {
        val path = "compiler/new-tests-infrastructure/testData/a.kt"
        val originalFile = File(path)
        val isFirIdentical = originalFile.useLines { it.first() == "// FIR_IDENTICAL" }
        val realPath = if (isFirIdentical) {
            path
        } else {
            val realPath = "${path.removeSuffix(".kt")}.fir.kt"
            val firFile = File(realPath)
            if (!firFile.exists()) {
                originalFile.copyTo(firFile)
            }
            realPath
        }
        testRunner(firAnalysisConfiguration).runTest(realPath)
    }

    private val classicAnalysisConfiguration: TestConfigurationBuilder.() -> Unit = {
        globalDefaults {
            frontend = FrontendKind.ClassicFrontend
            backend = BackendKind.NoBackend
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Source
        }

        assertions = JUnit5Assertions

        useConfigurators(::JvmEnvironmentConfigurator)
        useMetaInfoProcessors(::OldNewInferenceMetaInfoProcessor)

        useFrontendFacades(::ClassicFrontendFacade)
        useFrontendHandlers(::DeclarationsDumpHandler, ::ClassicDiagnosticsHandler)
    }

    @Test
    @TestMetadata("a.kt")
    fun testClassicAnalysisA() {
        testRunner(classicAnalysisConfiguration).runTest("compiler/new-tests-infrastructure/testData/a.kt")
    }

    private val blackBoxRunnerConfiguration: TestConfigurationBuilder.() -> Unit = {
        globalDefaults {
            frontend = FrontendKind.ClassicFrontend
            backend = BackendKind.IrBackend
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Binary
        }

        assertions = JUnit5Assertions

        useConfigurators(::JvmEnvironmentConfigurator)

        useFrontendFacades(::ClassicFrontendFacade)
        useFrontendHandlers(::DeclarationsDumpHandler, ::ClassicDiagnosticsHandler)
        useFrontend2BackendConverters(::ClassicFrontend2ClassicBackendConverter, ::ClassicFrontend2IrConverter)
        useBackendFacades(::ClassicJvmBackendFacade, ::JvmIrBackendFacade)
        useArtifactsHandlers(::JvmBoxRunner)
    }

    @Test
    @TestMetadata("boxTest.kt")
    fun testBBBoxTest() {
        testRunner(blackBoxRunnerConfiguration).runTest("compiler/new-tests-infrastructure/testData/boxTest.kt")
    }

    @Test
    @TestMetadata("boxWithRuntimeTest.kt")
    fun testBBBoxWithRuntimeTest() {
        testRunner(blackBoxRunnerConfiguration).runTest("compiler/new-tests-infrastructure/testData/boxWithRuntimeTest.kt")
    }

    @Test
    @TestMetadata("boxWithJdkTest.kt")
    fun testBBBoxWithJdkTest() {
        testRunner(blackBoxRunnerConfiguration).runTest("compiler/new-tests-infrastructure/testData/boxWithJdkTest.kt")
    }

    @Test
    @TestMetadata("multiModuleBox.kt")
    fun testBBMultiModuleBox() {
        testRunner(blackBoxRunnerConfiguration).runTest("compiler/new-tests-infrastructure/testData/multiModuleBox.kt")
    }

    private val firBlackBoxRunnerConfiguration: TestConfigurationBuilder.() -> Unit = {
        globalDefaults {
            frontend = FrontendKind.FIR
            backend = BackendKind.IrBackend
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Binary
        }

        assertions = JUnit5Assertions

        useConfigurators(::JvmEnvironmentConfigurator)

        useFrontendFacades(::FirFrontendFacade)
        useFrontendHandlers(::FirDumpHandler, ::FirDiagnosticsHandler)
        useFrontend2BackendConverters(::Fir2IrResultsConverter)
        useBackendFacades(::JvmIrBackendFacade)
        useArtifactsHandlers(::JvmBoxRunner)
    }

    @Test
    @TestMetadata("boxTest.kt")
    fun testFirBBBoxTest() {
        testRunner(firBlackBoxRunnerConfiguration).runTest("compiler/new-tests-infrastructure/testData/boxTest.kt")
    }

    @Test
    @TestMetadata("boxWithRuntimeTest.kt")
    fun testFirBBBoxWithRuntimeTest() {
        testRunner(firBlackBoxRunnerConfiguration).runTest("compiler/new-tests-infrastructure/testData/boxWithRuntimeTest.kt")
    }

    @Test
    @TestMetadata("boxWithJdkTest.kt")
    fun testFirBBBoxWithJdkTest() {
        testRunner(firBlackBoxRunnerConfiguration).runTest("compiler/new-tests-infrastructure/testData/boxWithJdkTest.kt")
    }

    @Test
    @TestMetadata("multiModuleBox.kt")
    fun testFirBBMultiModuleBox() {
        testRunner(firBlackBoxRunnerConfiguration).runTest("compiler/new-tests-infrastructure/testData/multiModuleBox.kt")
    }

    private val jsClassicBlackBoxRunnerConfiguration: TestConfigurationBuilder.() -> Unit = {
        globalDefaults {
            frontend = FrontendKind.ClassicFrontend
            backend = BackendKind.NoBackend
            targetPlatform = JsPlatforms.defaultJsPlatform
            dependencyKind = DependencyKind.Binary
        }

        assertions = JUnit5Assertions

        useConfigurators(::JsEnvironmentConfigurator)
        useFrontendFacades(::ClassicFrontendFacade)
        useFrontendHandlers(::DeclarationsDumpHandler, ::ClassicDiagnosticsHandler)
    }

    @Test
    @TestMetadata("jsTest.kt")
    fun testJsBBjsTest() {
        testRunner(jsClassicBlackBoxRunnerConfiguration).runTest("compiler/new-tests-infrastructure/testData/jsTest.kt")
    }
}
