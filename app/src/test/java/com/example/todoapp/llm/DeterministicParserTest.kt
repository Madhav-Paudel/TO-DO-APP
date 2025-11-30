package com.example.todoapp.llm

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for DeterministicParser
 * 
 * Tests regex-based command parsing for goals, tasks, and queries
 */
class DeterministicParserTest {

    // =====================================================================
    // GOAL CREATION TESTS
    // =====================================================================

    @Test
    fun `parse simple goal creation`() {
        val action = DeterministicParser.parse("create goal Learn Python")
        
        assertTrue(action is AssistantAction.CreateGoal)
        val goalAction = action as AssistantAction.CreateGoal
        assertEquals("Learn Python", goalAction.goalTitle)
        assertEquals(3, goalAction.durationMonths) // Default
        assertEquals(30, goalAction.dailyMinutes) // Default
    }

    @Test
    fun `parse goal with duration`() {
        val action = DeterministicParser.parse("create goal Learn Kotlin in 6 months")
        
        assertTrue(action is AssistantAction.CreateGoal)
        val goalAction = action as AssistantAction.CreateGoal
        assertEquals("Learn Kotlin", goalAction.goalTitle)
        assertEquals(6, goalAction.durationMonths)
        assertEquals(30, goalAction.dailyMinutes) // Default
    }

    @Test
    fun `parse goal with minutes`() {
        val action = DeterministicParser.parse("create goal Practice piano 45 minutes daily")
        
        assertTrue(action is AssistantAction.CreateGoal)
        val goalAction = action as AssistantAction.CreateGoal
        assertEquals("Practice piano", goalAction.goalTitle)
        assertEquals(45, goalAction.dailyMinutes)
    }

    @Test
    fun `parse goal with duration and minutes`() {
        val action = DeterministicParser.parse("create goal Master chess in 12 months 60 minutes per day")
        
        assertTrue(action is AssistantAction.CreateGoal)
        val goalAction = action as AssistantAction.CreateGoal
        assertEquals("Master chess", goalAction.goalTitle)
        assertEquals(12, goalAction.durationMonths)
        assertEquals(60, goalAction.dailyMinutes)
    }

    @Test
    fun `parse new goal variant`() {
        val action = DeterministicParser.parse("new goal Exercise regularly")
        
        assertTrue(action is AssistantAction.CreateGoal)
        val goalAction = action as AssistantAction.CreateGoal
        assertEquals("Exercise regularly", goalAction.goalTitle)
    }

    @Test
    fun `parse add goal variant`() {
        val action = DeterministicParser.parse("add goal Read 50 books")
        
        assertTrue(action is AssistantAction.CreateGoal)
        val goalAction = action as AssistantAction.CreateGoal
        assertEquals("Read 50 books", goalAction.goalTitle)
    }

    @Test
    fun `parse start goal variant`() {
        val action = DeterministicParser.parse("start goal Meditation practice in 2 months 15 minutes per day")
        
        assertTrue(action is AssistantAction.CreateGoal)
        val goalAction = action as AssistantAction.CreateGoal
        assertEquals("Meditation practice", goalAction.goalTitle)
        assertEquals(2, goalAction.durationMonths)
        assertEquals(15, goalAction.dailyMinutes)
    }

    // =====================================================================
    // TASK CREATION TESTS
    // =====================================================================

    @Test
    fun `parse simple task creation`() {
        val action = DeterministicParser.parse("add task Review notes")
        
        assertTrue(action is AssistantAction.CreateTask)
        val taskAction = action as AssistantAction.CreateTask
        assertEquals("Review notes", taskAction.taskTitle)
        assertEquals("today", taskAction.dueDate) // Default
        assertEquals(30, taskAction.minutes) // Default
    }

    @Test
    fun `parse task with tomorrow`() {
        val action = DeterministicParser.parse("add task Submit report tomorrow")
        
        assertTrue(action is AssistantAction.CreateTask)
        val taskAction = action as AssistantAction.CreateTask
        assertEquals("Submit report", taskAction.taskTitle)
        assertEquals("tomorrow", taskAction.dueDate)
    }

    @Test
    fun `parse task with today`() {
        val action = DeterministicParser.parse("create task Call mom today")
        
        assertTrue(action is AssistantAction.CreateTask)
        val taskAction = action as AssistantAction.CreateTask
        assertEquals("Call mom", taskAction.taskTitle)
        assertEquals("today", taskAction.dueDate)
    }

    @Test
    fun `parse task with minutes`() {
        val action = DeterministicParser.parse("add task Study algorithms 45 minutes")
        
        assertTrue(action is AssistantAction.CreateTask)
        val taskAction = action as AssistantAction.CreateTask
        assertEquals("Study algorithms", taskAction.taskTitle)
        assertEquals(45, taskAction.minutes)
    }

    @Test
    fun `parse task with for goal`() {
        val action = DeterministicParser.parse("add task Practice scales for Piano goal")
        
        assertTrue(action is AssistantAction.CreateTask)
        val taskAction = action as AssistantAction.CreateTask
        assertEquals("Practice scales", taskAction.taskTitle)
        assertEquals("Piano", taskAction.goalTitle)
    }

    @Test
    fun `parse new task variant`() {
        val action = DeterministicParser.parse("new task Go grocery shopping")
        
        assertTrue(action is AssistantAction.CreateTask)
        val taskAction = action as AssistantAction.CreateTask
        assertEquals("Go grocery shopping", taskAction.taskTitle)
    }

    // =====================================================================
    // TASK COMPLETION TESTS
    // =====================================================================

    @Test
    fun `parse complete task command`() {
        val action = DeterministicParser.parse("complete task Review notes")
        
        assertTrue(action is AssistantAction.CompleteTask)
        val completeAction = action as AssistantAction.CompleteTask
        assertEquals("Review notes", completeAction.taskTitle)
    }

    @Test
    fun `parse done task variant`() {
        val action = DeterministicParser.parse("done task Submit homework")
        
        assertTrue(action is AssistantAction.CompleteTask)
        val completeAction = action as AssistantAction.CompleteTask
        assertEquals("Submit homework", completeAction.taskTitle)
    }

    @Test
    fun `parse finish task variant`() {
        val action = DeterministicParser.parse("finish task Code review")
        
        assertTrue(action is AssistantAction.CompleteTask)
        val completeAction = action as AssistantAction.CompleteTask
        assertEquals("Code review", completeAction.taskTitle)
    }

    @Test
    fun `parse finished with task variant`() {
        val action = DeterministicParser.parse("finished with task Morning run")
        
        assertTrue(action is AssistantAction.CompleteTask)
        val completeAction = action as AssistantAction.CompleteTask
        assertEquals("Morning run", completeAction.taskTitle)
    }

    // =====================================================================
    // DELETE GOAL TESTS
    // =====================================================================

    @Test
    fun `parse delete goal command`() {
        val action = DeterministicParser.parse("delete goal Learn Spanish")
        
        assertTrue(action is AssistantAction.DeleteGoal)
        val deleteAction = action as AssistantAction.DeleteGoal
        assertEquals("Learn Spanish", deleteAction.goalTitle)
    }

    @Test
    fun `parse remove goal variant`() {
        val action = DeterministicParser.parse("remove goal Exercise routine")
        
        assertTrue(action is AssistantAction.DeleteGoal)
        val deleteAction = action as AssistantAction.DeleteGoal
        assertEquals("Exercise routine", deleteAction.goalTitle)
    }

    // =====================================================================
    // DELETE TASK TESTS
    // =====================================================================

    @Test
    fun `parse delete task command`() {
        val action = DeterministicParser.parse("delete task Buy groceries")
        
        assertTrue(action is AssistantAction.DeleteTask)
        val deleteAction = action as AssistantAction.DeleteTask
        assertEquals("Buy groceries", deleteAction.taskTitle)
    }

    @Test
    fun `parse remove task variant`() {
        val action = DeterministicParser.parse("remove task Clean room")
        
        assertTrue(action is AssistantAction.DeleteTask)
        val deleteAction = action as AssistantAction.DeleteTask
        assertEquals("Clean room", deleteAction.taskTitle)
    }

    // =====================================================================
    // PROGRESS QUERY TESTS
    // =====================================================================

    @Test
    fun `parse show progress command`() {
        val action = DeterministicParser.parse("show progress")
        
        assertTrue(action is AssistantAction.ShowProgress)
    }

    @Test
    fun `parse show my progress variant`() {
        val action = DeterministicParser.parse("show my progress")
        
        assertTrue(action is AssistantAction.ShowProgress)
    }

    @Test
    fun `parse what's my progress variant`() {
        val action = DeterministicParser.parse("what's my progress")
        
        assertTrue(action is AssistantAction.ShowProgress)
    }

    @Test
    fun `parse how am i doing variant`() {
        val action = DeterministicParser.parse("how am i doing")
        
        assertTrue(action is AssistantAction.ShowProgress)
    }

    @Test
    fun `parse my status variant`() {
        val action = DeterministicParser.parse("my status")
        
        assertTrue(action is AssistantAction.ShowProgress)
    }

    // =====================================================================
    // EDGE CASES AND NEGATIVE TESTS
    // =====================================================================

    @Test
    fun `parse returns null for random text`() {
        val action = DeterministicParser.parse("Hello, how are you?")
        
        assertNull(action)
    }

    @Test
    fun `parse returns null for partial commands`() {
        assertNull(DeterministicParser.parse("create"))
        assertNull(DeterministicParser.parse("goal"))
        assertNull(DeterministicParser.parse("add"))
    }

    @Test
    fun `parse is case insensitive`() {
        val action1 = DeterministicParser.parse("CREATE GOAL Test Goal")
        val action2 = DeterministicParser.parse("Create Goal Test Goal")
        val action3 = DeterministicParser.parse("create goal Test Goal")
        
        assertTrue(action1 is AssistantAction.CreateGoal)
        assertTrue(action2 is AssistantAction.CreateGoal)
        assertTrue(action3 is AssistantAction.CreateGoal)
    }

    @Test
    fun `parse handles extra whitespace`() {
        val action = DeterministicParser.parse("create   goal    Learn   Python")
        
        assertTrue(action is AssistantAction.CreateGoal)
        val goalAction = action as AssistantAction.CreateGoal
        // Title might have extra spaces, that's okay for this test
        assertTrue(goalAction.goalTitle.contains("Learn"))
        assertTrue(goalAction.goalTitle.contains("Python"))
    }

    @Test
    fun `parse handles goal with numbers in title`() {
        val action = DeterministicParser.parse("create goal Complete 100 pushups challenge")
        
        assertTrue(action is AssistantAction.CreateGoal)
        val goalAction = action as AssistantAction.CreateGoal
        assertEquals("Complete 100 pushups challenge", goalAction.goalTitle)
    }

    @Test
    fun `parse handles complex task with all parameters`() {
        val action = DeterministicParser.parse("add task Practice chords tomorrow 30 minutes for Guitar goal")
        
        assertTrue(action is AssistantAction.CreateTask)
        val taskAction = action as AssistantAction.CreateTask
        assertEquals("Practice chords", taskAction.taskTitle)
        assertEquals("tomorrow", taskAction.dueDate)
        assertEquals(30, taskAction.minutes)
        assertEquals("Guitar", taskAction.goalTitle)
    }
}
