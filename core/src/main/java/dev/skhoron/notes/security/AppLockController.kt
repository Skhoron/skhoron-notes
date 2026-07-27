package dev.skhoron.notes.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Раньше `SkhoronNavGraph` решал, показывать экран блокировки или нет, только один раз —
 * при первом построении графа. Если свернуть приложение и вернуться, экран блокировки
 * повторно не появлялся. Этот контроллер следит за уходом ВСЕГО приложения в фон
 * (через ProcessLifecycleOwner, а не Activity.onStop — так поворот экрана или переход
 * между Activity внутри самого приложения не считается "уходом в фон").
 */
class AppLockController(private val securityStore: SecurityStore) : DefaultLifecycleObserver {

    private val _shouldShowLock = MutableStateFlow(false)
    val shouldShowLock: StateFlow<Boolean> = _shouldShowLock

    override fun onStop(owner: LifecycleOwner) {
        if (securityStore.isConfigured()) {
            _shouldShowLock.value = true
        }
    }

    /** Вызывается после успешной разблокировки или сразу после навигации на экран Lock. */
    fun consume() {
        _shouldShowLock.value = false
    }

    /**
     * Подписывает контроллер на жизненный цикл всего процесса. Вынесено сюда (а не в
     * SkhoronNotesApp в модуле :app), чтобы :app не тянул зависимость androidx.lifecycle-process
     * напрямую — это деталь реализации :core.
     */
    fun register() {
        androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
}