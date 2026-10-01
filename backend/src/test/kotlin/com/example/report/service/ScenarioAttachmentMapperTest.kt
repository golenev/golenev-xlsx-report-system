package com.example.report.service

import com.example.report.dto.ScenarioAttachmentRequest
import com.example.report.dto.ScenarioRequest
import com.example.report.dto.ScenarioStepRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ScenarioAttachmentMapperTest {

    @Test
    fun `separate and assemble use one preorder step number sequence`() {
        val scenario = ScenarioRequest(
            listOf(
                ScenarioStepRequest(
                    number = 1,
                    text = "root",
                    attachments = listOf(ScenarioAttachmentRequest(name = "root log", content = "root content")),
                    subSteps = listOf(
                        ScenarioStepRequest(
                            number = 1,
                            text = "child",
                            attachments = listOf(ScenarioAttachmentRequest(name = "child log", content = "child content")),
                        ),
                    ),
                ),
                ScenarioStepRequest(number = 2, text = "second root", attachments = emptyList()),
            ),
        )

        val separated = ScenarioAttachmentMapper.separate("T-1", scenario)

        assertNull(separated.scenario.steps[0].attachments)
        assertNull(separated.scenario.steps[0].subSteps[0].attachments)
        assertEquals(listOf(1, 2), separated.attachments.map { it.stepNumber })

        val assembled = ScenarioAttachmentMapper.assemble(separated.scenario, separated.attachments)
        assertEquals(listOf(1, 3), assembled.steps.map { it.stepNumber })
        assertEquals(2, assembled.steps[0].subSteps.single().stepNumber)
        assertEquals("root content", assembled.steps[0].attachments.orEmpty().single().content)
        assertEquals("child content", assembled.steps[0].subSteps.single().attachments.orEmpty().single().content)
    }
}
