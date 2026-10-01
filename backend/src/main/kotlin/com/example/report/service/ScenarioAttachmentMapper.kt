package com.example.report.service

import com.example.report.dto.ScenarioAttachmentRequest
import com.example.report.dto.ScenarioRequest
import com.example.report.dto.ScenarioStepRequest
import com.example.report.entity.TestAttachmentEntity

data class SeparatedScenario(
    val scenario: ScenarioRequest,
    val attachments: List<TestAttachmentEntity>,
)

object ScenarioAttachmentMapper {
    fun separate(testId: String, scenario: ScenarioRequest): SeparatedScenario {
        var nextStepNumber = 1
        val attachments = mutableListOf<TestAttachmentEntity>()

        fun visit(steps: List<ScenarioStepRequest>): List<ScenarioStepRequest> = steps.map { step ->
            val stepNumber = nextStepNumber++
            step.attachments.orEmpty().forEach { attachment ->
                attachments += TestAttachmentEntity(
                    testId = testId,
                    stepNumber = stepNumber,
                    name = attachment.name,
                    mediaType = attachment.mediaType,
                    content = attachment.content.orEmpty(),
                    source = attachment.source,
                    sizeBytes = attachment.sizeBytes,
                )
            }
            step.copy(
                stepNumber = null,
                attachments = null,
                subSteps = visit(step.subSteps),
            )
        }

        return SeparatedScenario(ScenarioRequest(visit(scenario.steps)), attachments)
    }

    fun assemble(scenario: ScenarioRequest, attachments: List<TestAttachmentEntity>): ScenarioRequest {
        val byStep = attachments.groupBy(TestAttachmentEntity::stepNumber)
        var nextStepNumber = 1

        fun visit(steps: List<ScenarioStepRequest>): List<ScenarioStepRequest> = steps.map { step ->
            val stepNumber = nextStepNumber++
            step.copy(
                stepNumber = stepNumber,
                attachments = byStep[stepNumber]?.map { attachment ->
                    ScenarioAttachmentRequest(
                        name = attachment.name,
                        mediaType = attachment.mediaType,
                        content = attachment.content,
                        source = attachment.source,
                        sizeBytes = attachment.sizeBytes,
                    )
                } ?: step.attachments.orEmpty(),
                subSteps = visit(step.subSteps),
            )
        }

        return ScenarioRequest(visit(scenario.steps))
    }
}
