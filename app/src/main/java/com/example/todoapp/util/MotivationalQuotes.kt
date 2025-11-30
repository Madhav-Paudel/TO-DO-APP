package com.example.todoapp.util

import kotlin.random.Random

object MotivationalQuotes {
    private val quotes = listOf(
        "🚀 The secret of getting ahead is getting started.",
        "📚 Learning is a treasure that will follow its owner everywhere.",
        "💪 Success is the sum of small efforts repeated day in and day out.",
        "🎯 The only way to do great work is to love what you do.",
        "🌟 Believe you can and you're halfway there.",
        "⏰ The best time to plant a tree was 20 years ago. The second best time is now.",
        "🧠 Your mind is a garden, your thoughts are the seeds.",
        "📈 Progress, not perfection.",
        "🔥 Stay hungry, stay foolish.",
        "💡 Education is the most powerful weapon you can use to change the world.",
        "🏆 Champions keep playing until they get it right.",
        "✨ The expert in anything was once a beginner.",
        "🎓 An investment in knowledge pays the best interest.",
        "🌈 Every accomplishment starts with the decision to try.",
        "⭐ Dream big, start small, act now.",
        "🦋 What we learn with pleasure we never forget.",
        "🔑 The more that you read, the more things you will know.",
        "🌱 Growth is never by mere chance; it is the result of forces working together.",
        "💎 Hard work beats talent when talent doesn't work hard.",
        "🎨 Creativity is intelligence having fun."
    )

    private val studyTips = listOf(
        "💡 Try the Pomodoro Technique: 25 min study, 5 min break!",
        "🎵 Listening to lo-fi music can boost focus.",
        "💧 Stay hydrated! Your brain needs water to work well.",
        "🌙 Get enough sleep - it helps consolidate memories.",
        "✍️ Writing notes by hand improves retention.",
        "🔄 Review material within 24 hours to remember it longer.",
        "🎯 Break big tasks into smaller, manageable chunks.",
        "🧘 Take deep breaths before starting - it reduces anxiety.",
        "📱 Put your phone in another room while studying.",
        "🏃 A quick walk can boost your brain power!"
    )

    private val celebrations = listOf(
        "🎉 Amazing work! You're crushing it!",
        "⭐ Superstar! Keep that momentum going!",
        "🏆 You're on fire! Nothing can stop you!",
        "🌟 Incredible progress! You should be proud!",
        "💪 Beast mode activated! Well done!",
        "🚀 You're reaching for the stars!",
        "👏 Standing ovation for your dedication!",
        "🎊 Celebration time! You earned it!",
        "💯 Perfection! You're absolutely killing it!",
        "🥇 Gold medal performance today!"
    )

    fun getRandomQuote(): String = quotes[Random.nextInt(quotes.size)]
    
    fun getRandomTip(): String = studyTips[Random.nextInt(studyTips.size)]
    
    fun getRandomCelebration(): String = celebrations[Random.nextInt(celebrations.size)]
    
    fun getGreeting(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good morning! ☀️"
            hour < 17 -> "Good afternoon! 🌤️"
            hour < 21 -> "Good evening! 🌅"
            else -> "Night owl mode! 🦉"
        }
    }
    
    fun getMotivationalMessage(streak: Int, studyMinutesToday: Int, tasksCompleted: Int): String {
        return when {
            streak >= 7 -> "🔥 $streak day streak! You're unstoppable!"
            studyMinutesToday >= 60 -> "📚 Over an hour of study today! Impressive!"
            tasksCompleted >= 5 -> "✅ $tasksCompleted tasks done! Productivity champion!"
            streak >= 3 -> "📈 $streak days in a row! Building great habits!"
            studyMinutesToday >= 30 -> "💪 30+ minutes studied! Keep it up!"
            tasksCompleted >= 1 -> "🎯 You've completed $tasksCompleted task(s) today!"
            else -> getRandomQuote()
        }
    }
}
