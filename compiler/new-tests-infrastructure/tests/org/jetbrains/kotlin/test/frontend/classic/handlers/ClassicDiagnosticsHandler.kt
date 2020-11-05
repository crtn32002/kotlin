/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic.handlers

import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil
import org.jetbrains.kotlin.checkers.utils.DiagnosticsRenderingConfiguration
import org.jetbrains.kotlin.codeMetaInfo.model.DiagnosticCodeMetaInfo
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactoryImpl
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendSourceArtifacts
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.GlobalMetadataInfoHandler
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.globalMetadataInfoHandler
import org.jetbrains.kotlin.test.services.kotlinCoreEnvironmentProvider

class ClassicDiagnosticsHandler(testServices: TestServices) : ClassicFrontendAnalysisHandler(testServices) {
    private val globalMetadataInfoHandler: GlobalMetadataInfoHandler
        get() = testServices.globalMetadataInfoHandler

    @OptIn(ExperimentalStdlibApi::class)
    override fun processModule(module: TestModule, info: ClassicFrontendSourceArtifacts) {
        val diagnosticsPerFile = info.analysisResult.bindingContext.diagnostics.all().groupBy {
            it.psiFile
        }

        val configuration = DiagnosticsRenderingConfiguration(
            platform = null,
            withNewInference = true,
            languageVersionSettings = info.languageVersionSettings,
            skipDebugInfoDiagnostics = testServices.kotlinCoreEnvironmentProvider.getCompilerConfiguration(module).getBoolean(JVMConfigurationKeys.IR)
        )

        for (file in module.files) {
            val ktFile = info.psiFiles[file] ?: continue
            val diagnostics = diagnosticsPerFile[ktFile] ?: continue
            for (diagnostic in diagnostics) {
                globalMetadataInfoHandler.addMetadataInfosForFile(file, diagnostic.toMetaInfo(file))
            }
            processDebugInfoDiagnostics(configuration, file, ktFile, info)
        }
    }

    private fun Diagnostic.toMetaInfo(
        file: TestFile
    ): List<DiagnosticCodeMetaInfo> = textRanges.map { range ->
        val metaInfo = DiagnosticCodeMetaInfo(range, ClassicMetaInfoUtils.renderDiagnosticNoArgs, this)
        val existing = globalMetadataInfoHandler.getExistingMetaInfosForActualMetadata(file, metaInfo)
        if (existing.any { it.description != null }) {
            metaInfo.replaceRenderConfiguration(ClassicMetaInfoUtils.renderDiagnosticWithArgs)
        }
        metaInfo
    }

    private fun processDebugInfoDiagnostics(
        configuration: DiagnosticsRenderingConfiguration,
        file: TestFile,
        ktFile: KtFile,
        info: ClassicFrontendSourceArtifacts
    ) {
        val debugAnnotations = CheckerTestUtil.getDebugInfoDiagnostics(
            ktFile,
            info.analysisResult.bindingContext,
            markDynamicCalls = true,
            dynamicCallDescriptors = mutableListOf(),
            configuration,
            dataFlowValueFactory = DataFlowValueFactoryImpl(info.languageVersionSettings),
            info.analysisResult.moduleDescriptor as ModuleDescriptorImpl,
            diagnosedRanges = null
        )
        debugAnnotations.map { debugAnnotation ->
            globalMetadataInfoHandler.addMetadataInfosForFile(file, debugAnnotation.diagnostic.toMetaInfo(file))
        }
    }

    override fun processAfterAllModules() {}
}
