package com.example.todoapp.data.repository

import com.example.todoapp.data.local.GoalDao
import com.example.todoapp.data.local.GoalEntity
import kotlinx.coroutines.flow.Flow

class GoalRepository(private val goalDao: GoalDao) {
    val allActiveGoals: Flow<List<GoalEntity>> = goalDao.getAllActiveGoals()

    suspend fun getGoalById(id: Long): GoalEntity? {
        return goalDao.getGoalById(id)
    }

    suspend fun insertGoal(goal: GoalEntity): Long {
        return goalDao.insertGoal(goal)
    }

    suspend fun updateGoal(goal: GoalEntity) {
        goalDao.updateGoal(goal)
    }

    suspend fun deleteGoal(goal: GoalEntity) {
        goalDao.deleteGoal(goal)
    }

    suspend fun deleteGoalById(goalId: Long) {
        goalDao.deleteGoalById(goalId)
    }
}
