/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("invisible_reference", "invisible_member", "FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.buildKotlinProjectStructureMetadata
import kotlin.test.*

class JvmAndAndroidIntermediateSourceSetTest {

    private lateinit var project: ProjectInternal
    private lateinit var kotlin: KotlinMultiplatformExtension
    private lateinit var jvmAndAndroidMain: KotlinSourceSet

    @BeforeTest
    fun setup() {
        project = ProjectBuilder.builder().build() as ProjectInternal
        project.extensions.getByType(ExtraPropertiesExtension::class.java).set("kotlin.mpp.enableGranularSourceSetsMetadata", "true")

        project.plugins.apply("kotlin-multiplatform")
        project.plugins.apply("android-library")

        /* Arbitrary minimal Android setup */
        val android = project.extensions.getByName("android") as LibraryExtension
        android.compileSdkVersion(30)

        /* Kotlin Setup */
        kotlin = project.multiplatformExtension
        kotlin.jvm()
        kotlin.android()
        jvmAndAndroidMain = kotlin.sourceSets.create("jvmAndAndroidMain")
        kotlin.sourceSets.run {
            jvmAndAndroidMain.dependsOn(getByName("commonMain"))

            getByName("jvmMain") {
                it.dependsOn(jvmAndAndroidMain)
            }
            getByName("androidMain") {
                it.dependsOn(jvmAndAndroidMain)
            }
        }
    }

    @Test
    fun `metadata compilation is created and disabled`() {
        /* evaluate */
        project.evaluate()

        /* Check if compilation is created correctly */
        val jvmAndAndroidMainMetadataCompilations = kotlin.targets.flatMap { it.compilations }
            .filterIsInstance<KotlinMetadataCompilation<*>>()
            .filter { it.name == jvmAndAndroidMain.name }

        assertEquals(
            1, jvmAndAndroidMainMetadataCompilations.size,
            "Expected exactly one metadata compilation created for jvmAndAndroidMain source set"
        )

        val compilation = jvmAndAndroidMainMetadataCompilations.single()
        assertFalse(
            compilation.compileKotlinTaskProvider.get().enabled,
            "Expected compilation task to be disabled, because not supported yet"
        )
    }

    @Test
    fun `KotlinProjectStructureMetadata jvmAndAndroidMain exists in jvm variants`() {
        project.evaluate()
        val metadata = assertNotNull(buildKotlinProjectStructureMetadata(project))
        assertTrue("jvmAndAndroidMain" in metadata.sourceSetNamesByVariantName["jvmApiElements"].orEmpty())
        assertTrue("jvmAndAndroidMain" in metadata.sourceSetNamesByVariantName["jvmRuntimeElements"].orEmpty())
    }
}
