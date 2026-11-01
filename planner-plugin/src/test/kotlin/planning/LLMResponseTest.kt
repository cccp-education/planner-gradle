package planning

import kotlin.test.Test
import kotlin.test.assertEquals

class LLMResponseTest {
    @Test
    fun `toPlan maps title correctly`() {
        val response =
            LLMResponse(
                title = "Add test framework",
                epics = emptyList(),
                totalPoints = 0,
                estimatedSessions = "1-2",
            )
        val plan = response.toPlan()
        assertEquals("Add test framework", plan.title)
        assertEquals(0, plan.totalPoints)
        assertEquals("1-2", plan.estimatedSessions)
    }

    @Test
    fun `toPlan maps single epic with all levels`() {
        val response =
            LLMResponse(
                title = "Setup CI",
                epics =
                    listOf(
                        LLMEpic(
                            name = "CI-0",
                            description = "Bootstrap CI pipeline",
                            points = 5,
                            userStories =
                                listOf(
                                    LLMUserStory(
                                        description = "Add GitHub Actions workflow",
                                        tasks =
                                            listOf(
                                                LLMTask("Create workflow YAML", "./gradlew verify"),
                                                LLMTask("Test workflow trigger", "./gradlew check"),
                                            ),
                                    ),
                                    LLMUserStory(
                                        description = "Configure build scan",
                                        tasks =
                                            listOf(
                                                LLMTask("Add scan plugin", "./gradlew buildScan"),
                                            ),
                                    ),
                                ),
                        ),
                    ),
                totalPoints = 5,
                estimatedSessions = "2-3",
            )
        val plan = response.toPlan()

        assertEquals(1, plan.epics.size)
        assertEquals("CI-0", plan.epics[0].name)
        assertEquals("Bootstrap CI pipeline", plan.epics[0].description)
        assertEquals(5, plan.epics[0].points)

        assertEquals(2, plan.epics[0].userStories.size)

        val us1 = plan.epics[0].userStories[0]
        assertEquals("Add GitHub Actions workflow", us1.description)
        assertEquals(2, us1.tasks.size)
        assertEquals("Create workflow YAML", us1.tasks[0].description)
        assertEquals("./gradlew verify", us1.tasks[0].gradleTask)
        assertEquals("Test workflow trigger", us1.tasks[1].description)
        assertEquals("./gradlew check", us1.tasks[1].gradleTask)

        val us2 = plan.epics[0].userStories[1]
        assertEquals("Configure build scan", us2.description)
        assertEquals(1, us2.tasks.size)
        assertEquals("Add scan plugin", us2.tasks[0].description)
        assertEquals("./gradlew buildScan", us2.tasks[0].gradleTask)
    }

    @Test
    fun `toPlan maps multiple epics`() {
        val response =
            LLMResponse(
                title = "Full pipeline",
                epics =
                    listOf(
                        LLMEpic("A-0", "First", 3, emptyList()),
                        LLMEpic("B-1", "Second", 8, emptyList()),
                        LLMEpic("C-2", "Third", 5, emptyList()),
                    ),
                totalPoints = 16,
                estimatedSessions = "5-7",
            )
        val plan = response.toPlan()

        assertEquals(3, plan.epics.size)
        assertEquals("A-0", plan.epics[0].name)
        assertEquals("B-1", plan.epics[1].name)
        assertEquals("C-2", plan.epics[2].name)
        assertEquals(16, plan.totalPoints)
    }

    @Test
    fun `toPlan handles empty user stories`() {
        val response =
            LLMResponse(
                title = "Minimal",
                epics = listOf(LLMEpic("M-0", "Minimal epic", 1, emptyList())),
                totalPoints = 1,
                estimatedSessions = "1",
            )
        val plan = response.toPlan()
        assertEquals(1, plan.epics.size)
        assertEquals(0, plan.epics[0].userStories.size)
    }

    @Test
    fun `toPlan preserves field order`() {
        val response =
            LLMResponse(
                title = "Order test",
                epics =
                    listOf(
                        LLMEpic(
                            "X-0",
                            "Alpha",
                            1,
                            listOf(
                                LLMUserStory(
                                    "First US",
                                    listOf(
                                        LLMTask("First task", "./gradlew a"),
                                        LLMTask("Second task", "./gradlew b"),
                                    ),
                                ),
                                LLMUserStory(
                                    "Second US",
                                    listOf(
                                        LLMTask("Third task", "./gradlew c"),
                                    ),
                                ),
                            ),
                        ),
                    ),
                totalPoints = 1,
                estimatedSessions = "1",
            )
        val plan = response.toPlan()
        val usList = plan.epics[0].userStories
        assertEquals("First US", usList[0].description)
        assertEquals("Second US", usList[1].description)
        assertEquals("First task", usList[0].tasks[0].description)
        assertEquals("Second task", usList[0].tasks[1].description)
        assertEquals("Third task", usList[1].tasks[0].description)
    }
}
