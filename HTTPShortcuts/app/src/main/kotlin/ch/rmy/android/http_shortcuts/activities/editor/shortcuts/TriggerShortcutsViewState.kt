package ch.rmy.android.http_shortcuts.activities.editor.shortcuts

import ch.rmy.android.framework.viewmodel.viewstate.DialogState

data class TriggerShortcutsViewState(
    val dialogState: DialogState? = null,
    val shortcuts: List<ShortcutListItem> = emptyList(),
) {
    val isDraggingEnabled: Boolean
        get() = shortcuts.size > 1
}
