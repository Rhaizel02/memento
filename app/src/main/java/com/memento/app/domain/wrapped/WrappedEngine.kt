package com.memento.app.domain.wrapped

import com.memento.app.domain.model.MediaDetail
import com.memento.app.domain.model.MediaType
import com.memento.app.domain.stats.HighlightedWork
import com.memento.app.domain.stats.StatsEngine

sealed interface WrappedSlide {
    data class Cover(val year: Int) : WrappedSlide
    data class Completed(val count: Int, val byType: Map<MediaType, Int>) : WrappedSlide
    data class TopGenre(val name: String, val count: Int) : WrappedSlide
    data class BestRated(val work: HighlightedWork) : WrappedSlide
    data class TopCreator(val name: String, val count: Int) : WrappedSlide
    data class GameTime(val hours: Double) : WrappedSlide
    data class Reflections(val count: Int) : WrappedSlide
    data class ReflectionSpotlight(val workTitle: String, val content: String) : WrappedSlide
    data class Revisited(val work: HighlightedWork) : WrappedSlide
    data class Finale(val year: Int, val completed: Int, val favoriteCount: Int) : WrappedSlide
}

data class WrappedStory(val year: Int, val slides: List<WrappedSlide>)

object WrappedEngine {
    fun create(history: List<MediaDetail>, year: Int): WrappedStory {
        val summary = StatsEngine.calculate(history, year)
        val slides = buildList {
            add(WrappedSlide.Cover(year))
            add(WrappedSlide.Completed(summary.completedWorks, summary.completedByType))
            summary.topConsumedGenres.firstOrNull()?.let { add(WrappedSlide.TopGenre(it.label, it.count)) }
            summary.bestRatedWork?.let { add(WrappedSlide.BestRated(it)) }
            summary.frequentCreators.firstOrNull()?.let { add(WrappedSlide.TopCreator(it.label, it.count)) }
            summary.gameHours?.let { add(WrappedSlide.GameTime(it)) }
            if (summary.reflectionCount > 0) add(WrappedSlide.Reflections(summary.reflectionCount))
            summary.meaningfulReflection?.let { reflection ->
                val title = history.firstOrNull { detail ->
                    detail.consumptions.any { it.id == reflection.consumptionId }
                }?.media?.title.orEmpty()
                add(WrappedSlide.ReflectionSpotlight(title, reflection.content))
            }
            summary.mostRevisitedWork?.let { add(WrappedSlide.Revisited(it)) }
            add(WrappedSlide.Finale(year, summary.completedWorks, summary.favoriteWorks))
        }
        return WrappedStory(year, slides)
    }
}
