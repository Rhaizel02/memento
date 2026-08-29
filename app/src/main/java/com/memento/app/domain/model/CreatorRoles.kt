package com.memento.app.domain.model

fun MediaType.defaultCreatorRole(): CreatorRole = when (this) {
    MediaType.BOOK -> CreatorRole.AUTHOR
    MediaType.MOVIE -> CreatorRole.DIRECTOR
    MediaType.SERIES -> CreatorRole.CREATOR
    MediaType.GAME -> CreatorRole.DEVELOPER
}
