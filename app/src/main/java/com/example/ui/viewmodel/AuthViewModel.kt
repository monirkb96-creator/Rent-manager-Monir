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

    private val _preferredRole = MutableStateFlow<String?>(null)
    val preferredRole: StateFlow<String?> = _preferredRole.asStateFlow()

    fun setPreferredRole(role: String?) {
        _preferredRole.value = role
    }

    fun resolveForUser(user: FirebaseUser?, explicitRole: String? = _preferredRole.value) {
        if (user == null) {
            _sessionState.value = SessionResolution.Unauthenticated
            return
        }
        _sessionState.value = SessionResolution.Loading
        viewModelScope.launch {
            _sessionState.value = authRepository.resolveUserSession(user, explicitRole)
            _preferredRole.value = null
        }
    }

    fun initializeAsOwner(user: FirebaseUser) {
        _sessionState.value = SessionResolution.Loading
        viewModelScope.launch {
            _sessionState.value = authRepository.initializeAsOwner(user)
            _preferredRole.value = null
        }
    }

    fun upgradeCurrentTenantToOwner(user: FirebaseUser) {
        _sessionState.value = SessionResolution.Loading
        viewModelScope.launch {
            _sessionState.value = authRepository.upgradeUnassignedTenantToOwner(user)
            _preferredRole.value = null
        }
    }

    fun initializeAsTenant(user: FirebaseUser) {
        _sessionState.value = SessionResolution.Loading
        viewModelScope.launch {
            _sessionState.value = authRepository.initializeAsTenant(user)
            _preferredRole.value = null
        }
    }
}
