/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo
import org.jetbrains.kotlin.codeMetaInfo.renderConfigurations.AbstractCodeMetaInfoRenderConfiguration
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.rendering.AbstractDiagnosticWithParametersRenderer
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDefaultErrorMessages
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.test.frontend.fir.FirSourceArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.globalMetadataInfoHandler

class FirDiagnosticsHandler(testServices: TestServices) : FirAnalysisHandler(testServices) {
    private val renderConfiguration = FirDiagnosticCodeMetaRenderConfiguration()

    override fun processModule(module: TestModule, info: FirSourceArtifact) {
        val diagnosticsPerFile = info.firAnalyzerFacade.runCheckers()

        val globalMetadataInfoHandler = testServices.globalMetadataInfoHandler

        for (file in module.files) {
            val ktFile = info.firFiles[file] ?: continue
            val diagnostics = diagnosticsPerFile[ktFile] ?: continue
            val diagnosticsMetadataInfos = diagnostics.map {
                it.factory.psiDiagnosticFactory
                FirDiagnosticCodeMetaInfo(renderConfiguration, it)
            }
            globalMetadataInfoHandler.addMetadataInfosForFile(file, diagnosticsMetadataInfos)
        }
    }

    override fun processAfterAllModules() {}
}

private class FirDiagnosticCodeMetaRenderConfiguration(
    val renderSeverity: Boolean = false
) : AbstractCodeMetaInfoRenderConfiguration() {
    private val crossPlatformLineBreak = """\r?\n""".toRegex()

    override fun asString(codeMetaInfo: CodeMetaInfo): String {
        if (codeMetaInfo !is FirDiagnosticCodeMetaInfo) return ""
        return (getTag(codeMetaInfo)
                + getPlatformsString(codeMetaInfo)
                + getParamsString(codeMetaInfo))
            .replace(crossPlatformLineBreak, "")
    }

    private fun getParamsString(codeMetaInfo: FirDiagnosticCodeMetaInfo): String {
        if (!renderParams) return ""
        val params = mutableListOf<String>()

        val diagnostic = codeMetaInfo.diagnostic
        // TODO: support light tree diagnostics
        if (diagnostic !is FirPsiDiagnostic<*>) return ""

        @Suppress("UNCHECKED_CAST")
        val renderer = FirDefaultErrorMessages.getRendererForDiagnostic(diagnostic)
        if (renderer is AbstractDiagnosticWithParametersRenderer) {
            val renderParameters = (renderer as AbstractDiagnosticWithParametersRenderer<Diagnostic>).renderParameters(diagnostic.asPsiBasedDiagnostic())
            params.addAll(ContainerUtil.map(renderParameters) { it.toString() })
        }
        if (renderSeverity)
            params.add("severity='${diagnostic.severity}'")

        params.add(getAdditionalParams(codeMetaInfo))

        return "(\"${params.filter { it.isNotEmpty() }.joinToString("; ")}\")"
    }

    fun getTag(codeMetaInfo: FirDiagnosticCodeMetaInfo): String {
        return codeMetaInfo.diagnostic.factory.name
    }
}

private class FirDiagnosticCodeMetaInfo(
    override val renderConfiguration: FirDiagnosticCodeMetaRenderConfiguration,
    val diagnostic: FirDiagnostic<*>
) : CodeMetaInfo {
    override val start: Int
        get() = diagnostic.element.startOffset
    override val end: Int
        get() = diagnostic.element.endOffset

    override val tag: String
        get() = renderConfiguration.getTag(this)

    override val platforms: MutableList<String> = mutableListOf()

    override fun asString(): String = renderConfiguration.asString(this)
}
