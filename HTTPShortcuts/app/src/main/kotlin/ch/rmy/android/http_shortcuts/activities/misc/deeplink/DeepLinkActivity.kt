package ch.rmy.android.http_shortcuts.activities.misc.deeplink

import android.os.Bundle
import ch.rmy.android.framework.extensions.bindViewModel
import ch.rmy.android.framework.extensions.observe
import ch.rmy.android.framework.ui.Entrypoint
import ch.rmy.android.http_shortcuts.activities.BaseActivity

class DeepLinkActivity : BaseActivity(), Entrypoint {

    override val initializeWithTheme: Boolean
        get() = false

    private val viewModel: DeepLinkViewModel by bindViewModel()

    override fun onCreated(savedState: Bundle?) {
        viewModel.initialize(DeepLinkViewModel.InitData(intent.data))
        initViewModelBindings()
    }

    private fun initViewModelBindings() {
        viewModel.viewState.observe(this) { viewState ->
            setDialogState(viewState.dialogState, viewModel)
        }
        viewModel.events.observe(this, ::handleEvent)
    }
}
