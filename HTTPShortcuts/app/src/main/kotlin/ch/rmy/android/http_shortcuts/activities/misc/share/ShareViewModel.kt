package ch.rmy.android.http_shortcuts.activities.misc.share

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import ch.rmy.android.framework.extensions.attachTo
import ch.rmy.android.framework.extensions.mapFor
import ch.rmy.android.framework.viewmodel.BaseViewModel
import ch.rmy.android.framework.viewmodel.WithDialog
import ch.rmy.android.framework.viewmodel.viewstate.DialogState
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.ExecuteActivity
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.ShortcutRepository
import ch.rmy.android.http_shortcuts.data.domains.variables.VariableRepository
import ch.rmy.android.http_shortcuts.data.models.Shortcut
import ch.rmy.android.http_shortcuts.data.models.Variable
import ch.rmy.android.http_shortcuts.variables.VariableLookup
import ch.rmy.android.http_shortcuts.variables.VariableManager
import ch.rmy.android.http_shortcuts.variables.VariableResolver
import com.afollestad.materialdialogs.callbacks.onCancel

class ShareViewModel(application: Application) : BaseViewModel<ShareViewModel.InitData, ShareViewState>(application), WithDialog {

    private val shortcutRepository = ShortcutRepository()
    private val variableRepository = VariableRepository()

    private lateinit var shortcuts: List<Shortcut>
    private lateinit var variables: List<Variable>

    private val text: String
        get() = initData.text ?: ""
    private val title: String
        get() = initData.title ?: ""
    private val fileUris: List<Uri>
        get() = initData.fileUris

    override var dialogState: DialogState?
        get() = currentViewState.dialogState
        set(value) {
            updateViewState {
                copy(dialogState = value)
            }
        }

    override fun initViewState() = ShareViewState()

    override fun onInitializationStarted(data: InitData) {
        shortcutRepository.getShortcuts()
            .subscribe { shortcuts ->
                this.shortcuts = shortcuts
                variableRepository.getVariables()
                    .subscribe { variables ->
                        this.variables = variables
                        finalizeInitialization()
                    }
                    .attachTo(destroyer)
            }
            .attachTo(destroyer)
    }

    override fun onInitialized() {
        if (text.isEmpty()) {
            handleFileSharing()
        } else {
            handleTextSharing()
        }
    }

    private fun handleTextSharing() {
        val variableLookup = VariableManager(variables)
        val variables = getTargetableVariablesForTextSharing()
        val variableIds = variables.map { it.id }.toSet()
        val shortcuts = getTargetableShortcutsForTextSharing(variableIds, variableLookup)

        val variableValues = variables.associate { variable ->
            variable.key to when {
                variable.isShareText && variable.isShareTitle -> "$title - $text"
                variable.isShareTitle -> title
                else -> text
            }
        }
        when (shortcuts.size) {
            0 -> showInstructions(R.string.error_not_suitable_shortcuts)
            1 -> {
                executeShortcut(shortcuts[0].id, variableValues = variableValues)
            }
            else -> {
                showShortcutSelection(shortcuts, variableValues = variableValues)
            }
        }
    }

    private fun getTargetableVariablesForTextSharing() =
        variables
            .filter { it.isShareText || it.isShareTitle }
            .toSet()

    private fun getTargetableShortcutsForTextSharing(variableIds: Set<String>, variableLookup: VariableLookup): List<Shortcut> =
        shortcuts
            .filter { it.hasShareVariable(variableIds, variableLookup) }

    private fun getTargetableShortcutsForFileSharing(): List<Shortcut> =
        shortcuts
            .filter { it.hasFileParameter() || it.usesFileBody() }

    private fun handleFileSharing() {
        val shortcutsForFileSharing = getTargetableShortcutsForFileSharing()
        when (shortcutsForFileSharing.size) {
            0 -> {
                showInstructions(R.string.error_not_suitable_shortcuts)
            }
            1 -> {
                executeShortcut(shortcutsForFileSharing[0].id)
            }
            else -> showShortcutSelection(shortcutsForFileSharing)
        }
    }

    private fun executeShortcut(shortcutId: String, variableValues: Map<String, String> = emptyMap()) {
        openActivity(
            ExecuteActivity.IntentBuilder(shortcutId)
                .variableValues(variableValues)
                .files(fileUris)
        )
        finish(skipAnimation = true)
    }

    private fun showInstructions(@StringRes text: Int) {
        dialogState = DialogState.create {
            message(text)
                .positive(R.string.dialog_ok) {
                    onInstructionsCanceled()
                }
                .build()
                .onCancel {
                    onInstructionsCanceled()
                }
        }
    }

    private fun onInstructionsCanceled() {
        finish(skipAnimation = true)
    }

    private fun showShortcutSelection(shortcuts: List<Shortcut>, variableValues: Map<String, String> = emptyMap()) {
        dialogState = DialogState.create {
            mapFor(shortcuts) { shortcut ->
                item(name = shortcut.name, shortcutIcon = shortcut.icon) {
                    executeShortcut(shortcut.id, variableValues)
                }
            }
                .build()
                .onCancel {
                    onShortcutSelectionCanceled()
                }
        }
    }

    private fun onShortcutSelectionCanceled() {
        finish(skipAnimation = true)
    }

    data class InitData(
        val text: String?,
        val title: String?,
        val fileUris: List<Uri>,
    )

    companion object {

        private fun Shortcut.hasShareVariable(variableIds: Set<String>, variableLookup: VariableLookup): Boolean {
            val variableIdsInShortcut = VariableResolver.extractVariableIds(this, variableLookup)
            return variableIds.any { variableIdsInShortcut.contains(it) }
        }

        private fun Shortcut.hasFileParameter(): Boolean =
            parameters.any { it.isFileParameter || it.isFilesParameter }
    }
}
