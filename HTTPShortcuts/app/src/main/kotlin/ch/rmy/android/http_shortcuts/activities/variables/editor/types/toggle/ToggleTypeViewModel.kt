package ch.rmy.android.http_shortcuts.activities.variables.editor.types.toggle

import android.app.Application
import ch.rmy.android.framework.extensions.attachTo
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.variables.editor.types.BaseVariableTypeViewModel
import ch.rmy.android.http_shortcuts.dagger.getApplicationComponent
import ch.rmy.android.http_shortcuts.data.domains.variables.VariableRepository
import javax.inject.Inject

class ToggleTypeViewModel(application: Application) : BaseVariableTypeViewModel<Unit, ToggleTypeViewState>(application) {

    @Inject
    lateinit var variableRepository: VariableRepository

    init {
        getApplicationComponent().inject(this)
    }

    override fun initViewState() = ToggleTypeViewState(
        options = computeOptionList(),
    )

    private fun computeOptionList() =
        variable.options?.map { OptionItem(it.id, it.labelOrValue) } ?: emptyList()

    override fun onVariableChanged() {
        updateViewState {
            copy(options = computeOptionList())
        }
    }

    override fun onInitialized() {
        super.onInitialized()
        variableRepository.getObservableVariables()
            .subscribe { variables ->
                updateViewState {
                    copy(variables = variables)
                }
            }
            .attachTo(destroyer)
    }

    fun onAddButtonClicked() {
        emitEvent(ToggleTypeEvent.ShowAddDialog)
    }

    fun onOptionClicked(id: String) {
        val option = variable.options?.firstOrNull { it.id == id } ?: return
        emitEvent(
            ToggleTypeEvent.ShowEditDialog(
                optionId = id,
                value = option.value,
            )
        )
    }

    fun onAddDialogConfirmed(value: String) {
        performOperation(
            temporaryVariableRepository.addOption(label = "", value)
        )
    }

    fun onEditDialogConfirmed(optionId: String, value: String) {
        performOperation(
            temporaryVariableRepository.updateOption(optionId, label = "", value)
        )
    }

    fun onDeleteOptionSelected(optionId: String) {
        performOperation(
            temporaryVariableRepository.removeOption(optionId)
        )
    }

    fun onOptionMoved(optionId1: String, optionId2: String) {
        performOperation(
            temporaryVariableRepository.moveOption(optionId1, optionId2)
        )
    }

    override fun validate() =
        if ((variable.options?.size ?: 0) < 2) {
            showSnackbar(R.string.error_not_enough_toggle_values)
            false
        } else true
}
