package cn.fatalc.flyfish

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.widget.LinearLayout
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.io.Serializable

const val USER_PREFERENCES_NAME = "user_preferences"

val Context.dataStore by preferencesDataStore(name = USER_PREFERENCES_NAME)

class ServiceSettingsActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingViewModel =
            SettingViewModel(userPreferencesRepository = UserPreferencesRepository(dataStore))
        setContent {
            Theme {
                UserPreferences(
                    fragmentContext = this,
                    preferenceFragmentCompat = UserPreferencesFragment(settingViewModel),
                )
            }
        }
    }
}

@Composable
fun UserPreferences(
    fragmentContext: FragmentActivity,
    preferenceFragmentCompat: PreferenceFragmentCompat,
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            LinearLayout(it).apply { id = R.id.layout_setting }.also {
                fragmentContext.supportFragmentManager.commit {
                    add(it.id, preferenceFragmentCompat)
                }
            }
        },
    )
}

fun MutablePreferences.toUserPreferences(): UserPreferences {
    return UserPreferences().apply {
        this@toUserPreferences[PreferencesKeys.KEYWORDS]?.apply { keyword = this }
        this@toUserPreferences[PreferencesKeys.MAX_PERFORM]?.apply { maxPerformActions = this }
        this@toUserPreferences[PreferencesKeys.IGNORE_AFTER]?.apply { ignoreAfterSeconds = this }
        this@toUserPreferences[PreferencesKeys.IGNORED_PACKAGES]?.apply { ignorePackages = this }
    }
}

class UserPreferencesFragment(
    private val settingViewModel: SettingViewModel
) : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context

        val keywordsPreference =
            EditTextPreference(context).apply {
                isIconSpaceReserved = false
                key = getString(R.string.setting_find_keywords)
                title = key
                setOnBindEditTextListener { it.setSingleLine() }
                onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { _: Preference, any: Any ->
                        (any as? String)?.takeIf { it -> it.isNotEmpty() }?.let {
                            settingViewModel.setPreference(
                                PreferencesKeys.KEYWORDS,
                                it.split(KeywordsSplitter, ignoreCase = false, limit = 0).toSet()
                            )
                            true
                        } ?: false
                    }
            }

        val ignoreAfterPreference =
            ListPreference(context).apply {
                isIconSpaceReserved = false
                key = getString(R.string.setting_ignore_after)
                title = key
                entries = arrayOf("1s", "2s", "3s", "5s")
                entryValues = arrayOf("1", "2", "3", "5")
                onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { _: Preference, any: Any ->
                        (any as? String)?.toLong()?.let {
                            settingViewModel.setPreference(PreferencesKeys.IGNORE_AFTER, it)
                            true
                        }
                            ?: false
                    }
            }

        val maxPerformActionsPreference =
            ListPreference(context).apply {
                isIconSpaceReserved = false
                key = getString(R.string.setting_max_perform)
                title = key
                entries = arrayOf("1", "2", "3", "5")
                entryValues = arrayOf("1", "2", "3", "5")
                onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { _: Preference, any: Any ->
                        (any as? String)?.toInt()?.let {
                            settingViewModel.setPreference(PreferencesKeys.MAX_PERFORM, it)
                            true
                        }
                            ?: false
                    }
            }

        preferenceScreen =
            preferenceManager.createPreferenceScreen(context).apply {
                addPreference(keywordsPreference)
                addPreference(ignoreAfterPreference)
                addPreference(maxPerformActionsPreference)
            }

        val update =
            fun(userPreferences: UserPreferences) {
                keywordsPreference.apply {
                    text = userPreferences.keyword.joinToString(separator = KeywordsSplitter)
                    summary = text
                }
                ignoreAfterPreference.apply {
                    value = userPreferences.ignoreAfterSeconds.toString()
                    summary = entry
                }
                maxPerformActionsPreference.apply {
                    value = userPreferences.maxPerformActions.toString()
                    summary = entry
                }
            }

        lifecycleScope.launchWhenStarted {
            settingViewModel.settings.collect { update(it.toUserPreferences()) }
        }
    }
}

const val KeywordsSplitter = ","

@Parcelize
data class UserPreferences(
    var ignoreAfterSeconds: Long = 5L,
    var maxPerformActions: Int = 1,
    var keyword: Set<String> = setOf("跳过", "skip"),
    var ignorePackages: Set<String> = setOf(
        "com.android.systemui",
        "com.android.settings",
    )
) : Parcelable, Serializable

private object PreferencesKeys {
    val KEYWORDS = stringSetPreferencesKey("keywords")
    val IGNORE_AFTER = longPreferencesKey("ignore_after")
    val MAX_PERFORM = intPreferencesKey("max_perform")
    val IGNORED_PACKAGES = stringSetPreferencesKey("ignored_packages")
}

class UserPreferencesRepository(private val userPreferencesStore: DataStore<Preferences>) {

    val mutablePreferencesFlow: Flow<MutablePreferences> =
        userPreferencesStore.data.map { it.toMutablePreferences() }

    suspend fun <T> setPreference(key: Preferences.Key<T>, value: T) {
        userPreferencesStore.edit { it[key] = value }
    }
}

class SettingViewModel(private val userPreferencesRepository: UserPreferencesRepository) :
    ViewModel() {
    val settings = userPreferencesRepository.mutablePreferencesFlow

    fun <T> setPreference(key: Preferences.Key<T>, value: T) {
        viewModelScope.launch { userPreferencesRepository.setPreference(key, value) }
    }
}

@Preview(name = "preview accessibility setting")
@Composable
fun PreviewAccessibilitySetting() {
    Theme {}
}
