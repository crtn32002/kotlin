/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// TODO KT-34102
@file:Suppress("invisible_reference", "invisible_member", "FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle

import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.utils.AfterEvaluationQueue.Stage.*
import org.jetbrains.kotlin.gradle.utils.afterEvaluationQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AfterEvaluationQueueTest {

    @Test
    fun `extension returns same instance`() {
        val project = ProjectBuilder.builder().build()
        assertSame(project.afterEvaluationQueue, project.afterEvaluationQueue)
    }

    @Test
    fun `Stage PostProcessing is called after AfterEvaluate`() {
        val project = ProjectBuilder.builder().build()
        val queue = project.afterEvaluationQueue

        val postProcessingCalled = AtomicBoolean(false)
        val afterEvaluationCalled = AtomicBoolean(false)

        queue.schedule(PostProcessing) {
            assertFalse(postProcessingCalled.getAndSet(true), "Expected only one invocation(PostProcessing)")
            assertTrue(afterEvaluationCalled.get(), "Expected AfterEvaluation to be called already")
        }

        queue.schedule(AfterEvaluation) {
            assertFalse(afterEvaluationCalled.getAndSet(true), "Expected only one invocation (AfterEvaluation)")
            assertFalse(postProcessingCalled.get(), "Expected PostProcessing to *not* be called already")
        }

        project as ProjectInternal
        project.evaluate()

        assertTrue(afterEvaluationCalled.get(), "Expected afterEvaluation to be called")
        assertTrue(postProcessingCalled.get(), "Expected PostProcessing to be called")
    }

    @Test
    fun `AfterEvaluationQueue created after evaluation`() {
        val project = ProjectBuilder.builder().build() as ProjectInternal
        val afterEvaluateCalled = AtomicBoolean(false)
        project.afterEvaluate {
            assertFalse(afterEvaluateCalled.getAndSet(true), "Expected afterEvaluate to be called just once")
        }
        project.evaluate()

        val afterEvaluationQueueCalled = AtomicBoolean(false)
        project.afterEvaluationQueue.schedule {
            assertFalse(afterEvaluationQueueCalled.getAndSet(true), "Expected afterEvaluationQueue to be called just once")
        }
        assertTrue(afterEvaluationQueueCalled.get(), "Expected afterEvaluationQueue to be called inline")
    }

    @Test
    fun `AfterEvaluationQueue executes tasks immediate after evaluation`() {
        val project = ProjectBuilder.builder().build()
        val queue = project.afterEvaluationQueue
        project as ProjectInternal
        project.evaluate()

        values().forEach { stage ->
            val executed = AtomicBoolean(false)
            queue.schedule(stage) {
                assertFalse(executed.getAndSet(true), "Expected only one invocation($stage)")
            }
            assertTrue(executed.get(), "Expected immediate execution")
        }
    }

    @Test
    fun `scheduling AfterEvaluation during AfterEvaluation`() {
        val project = ProjectBuilder.builder().build() as ProjectInternal
        val queue = project.afterEvaluationQueue
        val outerBlockFinished = AtomicBoolean(false)
        val innerBlockFinished = AtomicBoolean(false)
        queue.schedule(AfterEvaluation) {
            queue.schedule(AfterEvaluation) {
                innerBlockFinished.getAndSet(true)
            }
            assertFalse(outerBlockFinished.getAndSet(true), "Expected outer block to be called only once")
            assertFalse(innerBlockFinished.get(), "Expected inner block to be put at the end of the queue")
        }

        project.evaluate()
        assertTrue(outerBlockFinished.get(), "Expected outer block to be called")
        assertTrue(innerBlockFinished.get(), "Expected inner block to be called")
    }

    @Test
    fun `scheduling AfterEvaluation during PostProcessing`() {
        val project = ProjectBuilder.builder().build() as ProjectInternal
        val queue = project.afterEvaluationQueue
        val outerBlockFinished = AtomicBoolean(false)
        val innerBlockFinished = AtomicBoolean(false)
        queue.schedule(PostProcessing) {
            queue.schedule(AfterEvaluation) {
                innerBlockFinished.getAndSet(true)
            }
            assertFalse(outerBlockFinished.getAndSet(true), "Expected outer block to be called only once")
            assertTrue(innerBlockFinished.get(), "Expected inner block to be executed immediately")
        }

        project.evaluate()
        assertTrue(outerBlockFinished.get(), "Expected outer block to be called")
        assertTrue(innerBlockFinished.get(), "Expected inner block to be called")
    }


    @Test
    fun `scheduling PostProcessing during AfterEvaluation`() {
        val project = ProjectBuilder.builder().build() as ProjectInternal
        val queue = project.afterEvaluationQueue
        val outerBlockFinished = AtomicBoolean(false)
        val innerBlockFinished = AtomicBoolean(false)
        queue.schedule(AfterEvaluation) {
            queue.schedule(PostProcessing) {
                innerBlockFinished.getAndSet(true)
            }
            assertFalse(outerBlockFinished.getAndSet(true), "Expected outer block to be called only once")
            assertFalse(innerBlockFinished.get(), "Expected inner block to be put at the end of the queue")
        }

        project.evaluate()
        assertTrue(outerBlockFinished.get(), "Expected outer block to be called")
        assertTrue(innerBlockFinished.get(), "Expected inner block to be called")
    }

    @Test
    fun `scheduling PostProcessing during PostProcessing`() {
        val project = ProjectBuilder.builder().build() as ProjectInternal
        val queue = project.afterEvaluationQueue
        val outerBlockFinished = AtomicBoolean(false)
        val innerBlockFinished = AtomicBoolean(false)
        queue.schedule(PostProcessing) {
            queue.schedule(PostProcessing) {
                innerBlockFinished.getAndSet(true)
            }
            assertFalse(outerBlockFinished.getAndSet(true), "Expected outer block to be called only once")
            assertFalse(innerBlockFinished.get(), "Expected inner block to be put at the end of the queue")
        }

        project.evaluate()
        assertTrue(outerBlockFinished.get(), "Expected outer block to be called")
        assertTrue(innerBlockFinished.get(), "Expected inner block to be called")
    }
}
