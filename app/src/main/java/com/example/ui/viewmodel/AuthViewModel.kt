package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.data.repository.SessionResolution
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _sessionState = MutableStateFlow<SessionResolution>(SessionResolution.Loading)
    val sessionState: StateFlow<SessionResolution> = _sessionState.asStateFlow()

    fun resolveForUser(user: FirebaseUser?) {
        if (user == null) {
            _sessionState.value = SessionResolution.Unauthenticated
            return
        }
        _sessionState.value = SessionResolution.Loading
        viewModelScope.launch {
            _sessionState.value = authRepository.resolveUserSession(user)
        }
    }

    fun initializeAsOwner(user: FirebaseUser) {
        _sessionState.value = SessionResolution.Loading
        viewModelScope.launch {
            _sessionState.value = authRepository.initializeAsOwner(user)
        }
    }

    fun initializeAsTenant(user: FirebaseUser) {
        _sessionState.value = SessionResolution.Loading
        viewModelScope.launch {
            _sessionState.value = authRepository.initializeAsTenant(user)
        }
    }
}
