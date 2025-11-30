package com.example.todoapp.llm

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for JsonResponseParser
 * 
 * Tests JSON parsing from LLM responses with various formats
 */
class JsonResponseParserTest {

    // =====================================================================
    // REPLY ACTION TESTS
    // =====================================================================

    @Test
    fun `parse simple reply action`() {
        val json = """{"action":"reply","message":"Hello! How can I help you?","data":{}}"""
        
        val action = JsonResponseParser.parseResponse(json)
        
        assertTrue(action is AssistantAction.Reply)
        val reply = action as AssistantAction.Reply
        assertEquals("Hello! How can I help you?", reply.message)
    }

    @Test
    fun `parse reply with extra whitespace`() {
        val json = """
            {
                "action": "reply",
                "message": "I understand. Let me help you with that.",
                "data": {}
            }
        """.trimIndent()
        
        val action = JsonResponseParser.parseResponse(json)
        
        assertTrue(action is AssistantAction.Reply)
    }

    // =====================================================================
    // CREATE GOAL ACTION TESTS
    // =====================================================================

    @Test
    fun `parse create goal action`() {
        val json = """{
            "action": "create_goal",
            "message": "I've created your goal!",
            "data": {
                "title": "Learn Python",
                "duration_months": 6,
                "daily_minutes": 45
            }
        }"""
        
        val action = JsonResponseParser.parseResponse(json)
        
        assertTrue(action is AssistantAction.CreateGoal)
        val goalAction = action as AssistantAction.CreateGoal
        assertEquals("Learn Python", goalAction.goalTitle)
        assertEquals(6, goalAction.durationMonths)
        assertEquals(45, goalAction.dailyMinutes)
        assertEquals("I've created your goal!", goalAction.message)
    }

    @Test
    fun `parse create goal with defaults`() {
        val json = """{
            "action": "create_goal",
            "message": "Goal created!",
            "data": {
                "title": "Exercise"
            }
        }"""
        
        val action = JsonResponseParser.parseResponse(json)
        
        assertTrue(action is AssistantAction.CreateGoal)
        val goalAction = action as AssistantAction.CreateGoal
        assertEquals("Exercise", goalAction.goalTitle)
        assertEquals(3, goalAction.durationMonths) // Default
        assertEquals(30, goalAction.dailyMinutes) // Default
    }

    // =====================================================================
    // CREATE TASK ACTION TESTS
    // =====================================================================

    @Test
    fun `parse create task action`() {
        val json = """{
            "action": "create_task",
            "message": "Task added!",
            "data": {
                "title": "Review notes",
                "due_date": "tomorrow",
                "minutes": 30,
                "goal_title": "Study"
            }
        }"""
        
        val action = JsonResponseParser.parseResponse(json)
        
        assertTrue(action is AssistantAction.CreateTask)
        val taskAction = action as AssistantAction.CreateTask
        assertEquals("Review notes", taskAction.taskTitle)
        assertEquals("tomorrow", taskAction.dueDate)
        assertEquals(30, taskAction.minutes)
        assertEquals("Study", taskAction.goalTitle)
    }

    @Test
    fun `parse create task with defaults`() {
        val json = """{
            "action": "create_task",
            "message": "Task created!",
            "data": {
                "title": "Buy groceries"
            }
        }"""
        
        val action = JsonResponseParser.parseResponse(json)
        
        assertTrue(action is AssistantAction.CreateTask)
        val taskAction = action as AssistantAction.CreateTask
        assertEquals("Buy groceries", taskAction.taskTitle)
        assertEquals("today", taskAction.dueDate) // Default
        assertEquals(30, taskAction.minutes) // Default
        assertNull(taskAction.goalTitle)
    }

    // =====================================================================
    // COMPLETE TASK ACTION TESTS
    // =====================================================================

    @Test
    fun `parse complete task action`() {
        val json = """{
            "action": "complete_task",
            "message": "Great job completing that task!",
            "data": {
                "title": "Morning exercise"
            }
        }"""
        
        val action = JsonResponseParser.parseResponse(json)
        
        assertTrue(action is AssistantAction.CompleteTask)
        val completeAction = action as AssistantAction.CompleteTask
        assertEquals("Morning exercise", completeAction.taskTitle)
        assertEquals("Great job completing that task!", completeAction.message)
    }

    // =====================================================================
    // DELETE GOAL ACTION TESTS
    // =====================================================================

    @Test
    fun `parse delete goal action`() {
        val json = """{
            "action": "delete_goal",
            "message": "Goal removed.",
            "data": {
                "title": "Old goal"
            }
        }"""
        
        val action = JsonResponseParser.parseResponse(json)
        
        assertTrue(action is AssistantAction.DeleteGoal)
        val deleteAction = action as AssistantAction.DeleteGoal
        assertEquals("Old goal", deleteAction.goalTitle)
    }

    // =====================================================================
    // DELETE TASK ACTION TESTS
    // =====================================================================

    @Test
    fun `parse delete task action`() {
        val json = """{
            "action": "delete_task",
            "message": "Task deleted.",
            "data": {
                "title": "Cancelled meeting"
            }
        }"""
        
        val action = JsonResponseParser.parseResponse(json)
        
        assertTrue(action is AssistantAction.DeleteTask)
        val deleteAction = action as AssistantAction.DeleteTask
        assertEquals("Cancelled meeting", deleteAction.taskTitle)
    }

    // =====================================================================
    // SHOW PROGRESS ACTION TESTS
    // =====================================================================

    @Test
    fun `parse show progress action`() {
        val json = """{
            "action": "show_progress",
            "message": "Here's your progress summary.",
            "data": {}
        }"""
        
        val action = JsonResponseParser.parseResponse(json)
        
        assertTrue(action is AssistantAction.ShowProgress)
        val progressAction = action as AssistantAction.ShowProgress
        assertEquals("Here's your progress summary.", progressAction.message)
    }

    // =====================================================================
    // JSON EXTRACTION TESTS
    // =====================================================================

    @Test
    fun `extract JSON from text with prefix`() {
        val response = """Sure, I'll help you with that!
            {"action":"reply","message":"Here is your answer.","data":{}}"""
        
        val action = JsonResponseParser.parseResponse(response)
        
        assertTrue(action is AssistantAction.Reply)
        assertEquals("Here is your answer.", (action as AssistantAction.Reply).message)
    }

    @Test
    fun `extract JSON from text with suffix`() {
        val response = """{"action":"reply","message":"Done!","data":{}}
            
            Let me know if you need anything else."""
        
        val action = JsonResponseParser.parseResponse(response)
        
        assertTrue(action is AssistantAction.Reply)
    }

    @Test
    fun `extract JSON from markdown code block`() {
        val response = """Here's my response:
            ```json
            {"action":"reply","message":"Found it!","data":{}}
            ```"""
        
        val action = JsonResponseParser.parseResponse(response)
        
        assertTrue(action is AssistantAction.Reply)
        assertEquals("Found it!", (action as AssistantAction.Reply).message)
    }

    // =====================================================================
    // ERROR HANDLING TESTS
    // =====================================================================

    @Test
    fun `handle invalid JSON gracefully`() {
        val invalidJson = "This is not JSON at all"
        
        val action = JsonResponseParser.parseResponse(invalidJson)
        
        assertTrue(action is AssistantAction.Reply)
        // Should return the original text as a reply
        assertTrue((action as AssistantAction.Reply).message.isNotBlank())
    }

    @Test
    fun `handle malformed JSON gracefully`() {
        val malformedJson = """{"action":"reply", "message":"""
        
        val action = JsonResponseParser.parseResponse(malformedJson)
        
        // Should not crash
        assertNotNull(action)
    }

    @Test
    fun `handle missing action field`() {
        val json = """{"message":"Hello!","data":{}}"""
        
        val action = JsonResponseParser.parseResponse(json)
        
        // Should default to reply
        assertTrue(action is AssistantAction.Reply)
    }

    @Test
    fun `handle unknown action type`() {
        val json = """{"action":"unknown_action","message":"Test","data":{}}"""
        
        val action = JsonResponseParser.parseResponse(json)
        
        // Should fall back to reply
        assertTrue(action is AssistantAction.Reply)
    }

    @Test
    fun `handle empty JSON object`() {
        val json = """{}"""
        
        val action = JsonResponseParser.parseResponse(json)
        
        assertNotNull(action)
    }

    @Test
    fun `handle empty string`() {
        val action = JsonResponseParser.parseResponse("")
        
        assertTrue(action is AssistantAction.Reply)
    }

    // =====================================================================
    // EDGE CASES
    // =====================================================================

    @Test
    fun `handle special characters in message`() {
        val json = """{
            "action": "reply",
            "message": "Here's a message with \"quotes\" and special chars: <>&",
            "data": {}
        }"""
        
        val action = JsonResponseParser.parseResponse(json)
        
        assertTrue(action is AssistantAction.Reply)
        assertTrue((action as AssistantAction.Reply).message.contains("quotes"))
    }

    @Test
    fun `handle unicode in message`() {
        val json = """{
            "action": "reply",
            "message": "Great job! 🎉 Keep it up! 💪",
            "data": {}
        }"""
        
        val action = JsonResponseParser.parseResponse(json)
        
        assertTrue(action is AssistantAction.Reply)
        assertTrue((action as AssistantAction.Reply).message.contains("🎉"))
    }

    @Test
    fun `handle nested data object`() {
        val json = """{
            "action": "create_goal",
            "message": "Created!",
            "data": {
                "title": "Complex goal",
                "duration_months": 3,
                "daily_minutes": 30,
                "extra": {
                    "nested": "value"
                }
            }
        }"""
        
        val action = JsonResponseParser.parseResponse(json)
        
        // Should still parse correctly, ignoring extra fields
        assertTrue(action is AssistantAction.CreateGoal)
        assertEquals("Complex goal", (action as AssistantAction.CreateGoal).goalTitle)
    }
}
