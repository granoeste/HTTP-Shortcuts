package ch.rmy.android.http_shortcuts.activities.editor.basicsettings

import android.app.Application
import ch.rmy.android.framework.extensions.attachTo
import ch.rmy.android.framework.viewmodel.BaseViewModel
import ch.rmy.android.http_shortcuts.activities.editor.basicsettings.usecases.GetAvailableBrowserPackageNamesUseCase
import ch.rmy.android.http_shortcuts.dagger.getApplicationComponent
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.TemporaryShortcutRepository
import ch.rmy.android.http_shortcuts.data.domains.variables.VariableRepository
import ch.rmy.android.http_shortcuts.data.enums.ShortcutExecutionType
import ch.rmy.android.http_shortcuts.data.models.ShortcutModel
import ch.rmy.android.http_shortcuts.extensions.type
import javax.inject.Inject

class BasicRequestSettingsViewModel(application: Application) : BaseViewModel<Unit, BasicRequestSettingsViewState>(application) {

    @Inject
    lateinit var temporaryShortcutRepository: TemporaryShortcutRepository

    @Inject
    lateinit var variableRepository: VariableRepository

    @Inject
    lateinit var getAvailableBrowserPackageNames: GetAvailableBrowserPackageNamesUseCase

    init {
        getApplicationComponent().inject(this)
    }

    override fun onInitializationStarted(data: Unit) {
        finalizeInitialization(silent = true)
    }

    override fun initViewState() = BasicRequestSettingsViewState()

    override fun onInitialized() {
        temporaryShortcutRepository.getTemporaryShortcut()
            .subscribe(
                ::initViewStateFromShortcut,
                ::onInitializationError,
            )
            .attachTo(destroyer)

        variableRepository.getObservableVariables()
            .subscribe { variables ->
                updateViewState {
                    copy(variables = variables)
                }
            }
            .attachTo(destroyer)
    }

    private fun initViewStateFromShortcut(shortcut: ShortcutModel) {
        updateViewState {
            copy(
                methodVisible = shortcut.type == ShortcutExecutionType.APP,
                browserPackageNameVisible = shortcut.type == ShortcutExecutionType.BROWSER,
                method = shortcut.method,
                url = shortcut.url,
                browserPackageName = shortcut.browserPackageName,
                browserPackageNameOptions = if (shortcut.type == ShortcutExecutionType.BROWSER) {
                    getAvailableBrowserPackageNames(shortcut.browserPackageName)
                } else {
                    emptyList()
                },
            )
        }
    }

    private fun onInitializationError(error: Throwable) {
        handleUnexpectedError(error)
        finish()
    }

    fun onBackPressed() {
        waitForOperationsToFinish {
            finish()
        }
    }

    fun onUrlChanged(url: String) {
        updateViewState {
            copy(url = url)
        }
        performOperation(
            temporaryShortcutRepository.setUrl(url)
        )
    }

    fun onMethodChanged(method: String) {
        updateViewState {
            copy(method = method)
        }
        performOperation(
            temporaryShortcutRepository.setMethod(method)
        )
    }

    fun onBrowserPackageNameChanged(packageName: String) {
        updateViewState {
            copy(browserPackageName = packageName)
        }
        performOperation(
            temporaryShortcutRepository.setBrowserPackageName(packageName)
        )
    }
}
