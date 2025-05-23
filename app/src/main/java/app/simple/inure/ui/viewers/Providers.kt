package app.simple.inure.ui.viewers

import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import app.simple.inure.R
import app.simple.inure.adapters.viewers.AdapterProviders
import app.simple.inure.constants.BundleConstants
import app.simple.inure.constants.Warnings
import app.simple.inure.decorations.overscroll.CustomVerticalRecyclerView
import app.simple.inure.dialogs.action.ComponentState
import app.simple.inure.dialogs.action.ComponentState.Companion.showComponentStateDialog
import app.simple.inure.extensions.fragments.SearchBarScopedFragment
import app.simple.inure.extensions.popup.PopupMenuCallback
import app.simple.inure.factories.panels.PackageInfoFactory
import app.simple.inure.models.ProviderInfoModel
import app.simple.inure.popups.viewers.PopupProvidersMenu
import app.simple.inure.preferences.ConfigurationPreferences
import app.simple.inure.preferences.ProvidersPreferences
import app.simple.inure.ui.subviewers.ProviderInfo
import app.simple.inure.viewmodels.viewers.ProvidersViewModel

class Providers : SearchBarScopedFragment() {

    private lateinit var recyclerView: CustomVerticalRecyclerView

    private var adapterProviders: AdapterProviders? = null
    private lateinit var providersViewModel: ProvidersViewModel
    private lateinit var packageInfoFactory: PackageInfoFactory

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_provider, container, false)

        recyclerView = view.findViewById(R.id.providers_recycler_view)
        search = view.findViewById(R.id.providers_search_btn)
        searchBox = view.findViewById(R.id.providers_search)
        title = view.findViewById(R.id.providers_title)

        packageInfoFactory = PackageInfoFactory(packageInfo)
        providersViewModel = ViewModelProvider(this, packageInfoFactory)[ProvidersViewModel::class.java]

        startPostponedEnterTransition()
        searchBoxState(animate = false, ProvidersPreferences.isSearchVisible())

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        providersViewModel.getProviders().observe(viewLifecycleOwner) {
            adapterProviders = AdapterProviders(it, packageInfo, searchBox.text.toString().trim())
            setCount(it.size)

            adapterProviders?.setOnProvidersCallbackListener(object : AdapterProviders.Companion.ProvidersCallbacks {
                override fun onProvidersClicked(providerInfoModel: ProviderInfoModel) {
                    openFragmentSlide(ProviderInfo.newInstance(providerInfoModel, packageInfo), ProviderInfo.TAG)
                }

                override fun onProvidersLongPressed(packageId: String, packageInfo: PackageInfo, icon: View, isComponentEnabled: Boolean, position: Int) {
                    if (ConfigurationPreferences.isRootOrShizuku()) {
                        PopupProvidersMenu(requireView(), isComponentEnabled).setOnMenuClickListener(object : PopupMenuCallback {
                            override fun onMenuItemClicked(source: String) {
                                when (source) {
                                    getString(R.string.enable), getString(R.string.disable) -> {
                                        showComponentStateDialog(packageInfo, packageId, isComponentEnabled, object : ComponentState.Companion.ComponentStatusCallbacks {
                                            override fun onSuccess() {
                                                adapterProviders?.notifyItemChanged(position)
                                            }
                                        })
                                    }
                                }
                            }
                        })
                    } else {
                        showWarning(Warnings.ROOT_OR_SHIZUKU_REQUIRED, false)
                    }
                }
            })

            recyclerView.setExclusiveAdapter(adapterProviders)
        }

        searchBox.doOnTextChanged { text, _, _, _ ->
            if (searchBox.isFocused) {
                providersViewModel.getProvidersData(text.toString().trim())
            }
        }

        providersViewModel.getError().observe(viewLifecycleOwner) {
            showError(it)
        }

        providersViewModel.notFound.observe(viewLifecycleOwner) {
            showWarning(R.string.no_providers_found)
        }

        search.setOnClickListener {
            if (searchBox.text.isNullOrEmpty()) {
                ProvidersPreferences.setSearchVisibility(!ProvidersPreferences.isSearchVisible())
            } else {
                searchBox.text?.clear()
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            ProvidersPreferences.PROVIDERS_SEARCH -> {
                searchBoxState(animate = true, ProvidersPreferences.isSearchVisible())
            }
        }
    }

    companion object {
        fun newInstance(packageInfo: PackageInfo, keywords: String? = null): Providers {
            val args = Bundle()
            args.putParcelable(BundleConstants.packageInfo, packageInfo)
            args.putString(BundleConstants.keywords, keywords)
            val fragment = Providers()
            fragment.arguments = args
            return fragment
        }

        const val TAG = "providers"
    }
}
