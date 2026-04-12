package br.com.tec.tecmotors.presentation.common

sealed interface UiFeedback {
    val message: String

    data class Success(override val message: String) : UiFeedback

    data class Error(override val message: String) : UiFeedback

    data class Info(override val message: String) : UiFeedback
}
