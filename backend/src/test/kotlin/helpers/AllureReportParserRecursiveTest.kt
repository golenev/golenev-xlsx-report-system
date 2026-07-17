package helpers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AllureReportParserRecursiveTest {
    @Test
    fun `parser preserves arbitrary nested steps metadata and ten attachments`() {
        val attachments = (1..10).map { index ->
            AllureUpload("source-$index.txt", "content-$index".toByteArray())
        }
        val attachmentJson = (1..10).joinToString(",") { index ->
            """{"name":"Attachment $index","source":"source-$index.txt","type":"text/plain"}"""
        }
        val report = """
            {
              "name":"Recursive scenario",
              "status":"passed",
              "labels":[{"name":"AS_ID","value":"500"},{"name":"suite","value":"UI"}],
              "testStage":{"steps":[{
                "name":"root","start":100,"stop":62101,"steps":[{
                  "name":"level-2","start":200,"stop":4301,"steps":[{
                    "name":"level-3","steps":[{
                      "name":"level-4","steps":[{
                        "name":"leaf","start":500,"stop":524,
                        "parameters":[{"name":"expectedDate","value":"2026-07-16"}],
                        "attachments":[$attachmentJson]
                      }]
                    }]
                  }]
                }]
              }]}
            }
        """.trimIndent()

        val scenario = parseAllureReportsFromUploads(
            listOf(AllureUpload("result.json", report.toByteArray())) + attachments
        ).single().scenario
        val root = scenario.steps.single()
        val leaf = root.subSteps.single().subSteps.single().subSteps.single().subSteps.single()

        assertEquals(62_001, root.durationMs)
        assertEquals(4_101, root.subSteps.single().durationMs)
        assertNull(root.subSteps.single().subSteps.single().durationMs)
        assertEquals(24, leaf.durationMs)
        assertEquals("expectedDate", leaf.parameters.single().name)
        assertEquals("2026-07-16", leaf.parameters.single().value)
        assertEquals(10, leaf.attachments?.size)
        assertEquals((1..10).map { "Attachment $it" }, leaf.attachments?.map { it.name })
        assertEquals((1..10).map { "content-$it" }, leaf.attachments?.map { it.content })
        assertEquals((1..10).map { "source-$it.txt" }, leaf.attachments?.map { it.source })
    }
}
