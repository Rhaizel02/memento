package com.memento.app.domain.usecase

import com.memento.app.domain.model.ProgressType

object ProgressValidator {
    fun validate(
        type: ProgressType,
        currentValue: Double?,
        totalValue: Double?,
        season: Int?,
        episode: Int?,
    ) {
        require(currentValue == null || currentValue.isFinite()) { "El progreso debe ser un número finito" }
        require(totalValue == null || totalValue.isFinite()) { "El total debe ser un número finito" }
        when (type) {
            ProgressType.PAGES -> {
                require(currentValue != null && currentValue >= 0) { "Las páginas actuales son obligatorias" }
                require(totalValue == null || totalValue > 0) { "El total de páginas debe ser positivo" }
                require(totalValue == null || currentValue <= totalValue) { "Las páginas actuales no pueden superar el total" }
            }
            ProgressType.EPISODE -> {
                require(season != null && season >= 1) { "La temporada debe ser 1 o superior" }
                require(episode != null && episode >= 1) { "El episodio debe ser 1 o superior" }
            }
            ProgressType.HOURS -> {
                require(currentValue != null && currentValue >= 0) { "Las horas son obligatorias" }
                require(totalValue == null || totalValue in 0.0..100.0) { "El porcentaje aproximado debe estar entre 0 y 100" }
            }
            ProgressType.PERCENT -> require(currentValue != null && currentValue in 0.0..100.0) {
                "El porcentaje debe estar entre 0 y 100"
            }
            ProgressType.MINUTES -> require(currentValue != null && currentValue >= 0) { "Los minutos son obligatorios" }
        }
    }
}
