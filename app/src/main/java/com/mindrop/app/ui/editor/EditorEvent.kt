package com.mindrop.app.ui.editor

sealed interface EditorEvent {
    data object Saved : EditorEvent
}
