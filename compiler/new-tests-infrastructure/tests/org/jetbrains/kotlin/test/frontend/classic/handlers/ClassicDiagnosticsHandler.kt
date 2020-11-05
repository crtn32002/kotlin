/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic.handlers

import org.jetbrains.kotlin.codeMetaInfo.model.DiagnosticCodeMetaInfo
import org.jetbrains.kotlin.codeMetaInfo.renderConfigurations.DiagnosticCodeMetaInfoRenderConfiguration
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendSourceArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.GlobalMetadataInfoHandler
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.globalMetadataInfoHandler

class ClassicDiagnosticsHandler(testServices: TestServices) : ClassicFrontendAnalysisHandler(testServices) {
    private val renderConfigurationNoArgs = DiagnosticCodeMetaInfoRenderConfiguration().apply { renderParams = false }
    private val renderConfigurationWithArgs = DiagnosticCodeMetaInfoRenderConfiguration().apply { renderParams = true }
    private val globalMetadataInfoHandler: GlobalMetadataInfoHandler
        get() = testServices.globalMetadataInfoHandler

    @OptIn(ExperimentalStdlibApi::class)
    override fun processModule(module: TestModule, info: ClassicFrontendSourceArtifacts) {
        val diagnosticsPerFile = info.analysisResult.bindingContext.diagnostics.all().groupBy {
            it.psiFile
        }

        for (file in module.files) {
            val ktFile = info.psiFiles[file] ?: continue
            val diagnostics = diagnosticsPerFile[ktFile] ?: continue
            val diagnosticsMetadataInfos = diagnostics.flatMap { diagnostic ->
                diagnostic.textRanges.map { range ->
                    val metaInfo = DiagnosticCodeMetaInfo(range, renderConfigurationNoArgs, diagnostic)
                    val existing = globalMetadataInfoHandler.getExistingMetaInfosForActualMetadata(file, metaInfo)
                    if (existing.any { it.description != null }) {
                        metaInfo.replaceRenderConfiguration(renderConfigurationWithArgs)
                    }
                    metaInfo
                }
            }
            globalMetadataInfoHandler.addMetadataInfosForFile(file, diagnosticsMetadataInfos)
        }
    }

    override fun processAfterAllModules() {}
}
