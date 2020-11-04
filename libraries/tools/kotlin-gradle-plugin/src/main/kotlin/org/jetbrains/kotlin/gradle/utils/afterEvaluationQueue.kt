/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

private typealias Action = Project.() -> Unit

internal val Project.afterEvaluationQueue: AfterEvaluationQueue
    get() = project.convention.findByName("evaluationQueue")?.run { this as AfterEvaluationQueue }
        ?: AfterEvaluationQueue(project).also { queue ->
            project.convention.add(AfterEvaluationQueue::class.java, "evaluationQueue", queue)
        }

internal class AfterEvaluationQueue(private val project: Project) {
    enum class Stage {
        AfterEvaluation, PostProcessing
    }

    private var latestExecutedStage: Stage? = null

    private val queues: Map<Stage, Queue<Action>> = Stage.values().toList().associateWith { LinkedList<Action>() }

    fun schedule(stage: Stage = Stage.AfterEvaluation, action: Project.() -> Unit) {
        val latestExecutedStage = this.latestExecutedStage
        if (latestExecutedStage != null && latestExecutedStage.ordinal >= stage.ordinal) project.action()
        else queues.getValue(stage).add(action)
    }

    private fun processQueues() {
        Stage.values().forEach { stage ->
            val queue = queues.getValue(stage)
            do {
                val action = queue.poll()
                action?.invoke(project)
            } while (action != null)
            latestExecutedStage = stage
        }
        assert(queues.values.all { it.isEmpty() }) {
            "AfterEvaluationQueue expected all queues to be empty after processing"
        }
    }

    init {
        if (project.state.executed) {
            latestExecutedStage = Stage.values().last()
        } else {
            project.scheduleAfterEvaluation {
                processQueues()
            }
        }
    }
}

private fun Project.scheduleAfterEvaluation(action: () -> Unit) {
    assert(!project.state.executed)
    val isDispatched = AtomicBoolean(false)
    androidPluginIds.forEach { androidPluginId ->
        pluginManager.withPlugin(androidPluginId) {
            if (!isDispatched.getAndSet(true)) {
                afterEvaluate { action() }
            }
        }
    }
    afterEvaluate {
        if (!isDispatched.getAndSet(true)) {
            action()
        }
    }
}
