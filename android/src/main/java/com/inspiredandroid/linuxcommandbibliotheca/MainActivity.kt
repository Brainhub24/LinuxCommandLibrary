package com.inspiredandroid.linuxcommandbibliotheca

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.inspiredandroid.linuxcommandbibliotheca.ui.composables.BottomBar
import com.inspiredandroid.linuxcommandbibliotheca.ui.composables.TopBar
import com.inspiredandroid.linuxcommandbibliotheca.ui.screens.basiccategories.BasicCategoriesScreen
import com.inspiredandroid.linuxcommandbibliotheca.ui.screens.basicgroups.BasicGroupsScreen
import com.inspiredandroid.linuxcommandbibliotheca.ui.screens.basicgroups.BasicGroupsViewModel
import com.inspiredandroid.linuxcommandbibliotheca.ui.screens.commanddetail.CommandDetailScreen
import com.inspiredandroid.linuxcommandbibliotheca.ui.screens.commanddetail.CommandDetailViewModel
import com.inspiredandroid.linuxcommandbibliotheca.ui.screens.commandlist.CommandListScreen
import com.inspiredandroid.linuxcommandbibliotheca.ui.screens.commandlist.CommandListViewModel
import com.inspiredandroid.linuxcommandbibliotheca.ui.screens.tips.TipsScreen
import com.inspiredandroid.linuxcommandbibliotheca.ui.theme.LinuxTheme
import com.linuxcommandlibrary.shared.hasDatabase
import com.linuxcommandlibrary.shared.initDatabase
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

/* Copyright 2022 Simon Schubert
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

const val deepLinkUri = "https://linuxcommandlibrary.com"

class MainActivity : AppCompatActivity() {

    private val preferenceManager by inject<PreferenceUtil>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasDatabase(this) || !preferenceManager.isDatabaseUpToDate(this)) {
            startActivity(Intent(this, InitializeDatabaseActivity::class.java))
            preferenceManager.updateDatabaseVersion(this)
            finish()
            return
        }

        initDatabase(this)

        setContent {
            LinuxApp()
        }
    }

    @Composable
    fun LinuxApp() {
        val navController = rememberNavController()
        val navBackStackEntry = navController.currentBackStackEntryAsState()
        val searchTextValue = remember {
            mutableStateOf(
                TextFieldValue(text = "", selection = TextRange(0))
            )
        }
        val onNavigate: (String) -> Unit = {
            navController.navigate(it)
        }

        LinuxTheme {
            Scaffold(
                topBar = {
                    TopBar(navBackStackEntry,
                        searchTextValue,
                        preferenceManager,
                        onNavigateBack = {
                            navController.popBackStack()
                        })
                },
                bottomBar = {
                    BottomBar(navController)
                }) { innerPadding ->

                NavHost(
                    navController,
                    startDestination = Screen.Basics.route,
                    Modifier.padding(innerPadding)
                ) {

                    composable(
                        Screen.Basics.route,
                        deepLinks = listOf(
                            navDeepLink { uriPattern = "$deepLinkUri/basics" },
                            navDeepLink { uriPattern = "$deepLinkUri/basics.html" })
                    ) {
                        BasicCategoriesScreen(onNavigate)
                    }
                    composable(
                        Screen.Commands.route,
                        deepLinks = listOf(
                            navDeepLink { uriPattern = "$deepLinkUri/" },
                            navDeepLink { uriPattern = "$deepLinkUri/index.html" })
                    ) {
                        val viewModel = getViewModel<CommandListViewModel>()

                        CommandListScreen(
                            searchTextValue.value.text,
                            viewModel,
                            onNavigate
                        )
                    }
                    composable(
                        Screen.Tips.route,
                        deepLinks = listOf(
                            navDeepLink { uriPattern = "$deepLinkUri/tips" },
                            navDeepLink { uriPattern = "$deepLinkUri/tips.html" })
                    ) {
                        TipsScreen(onNavigate)
                    }
                    composable(
                        "basicgroups?categoryId={categoryId}&categoryName={categoryName}",
                        arguments = listOf(
                            navArgument("categoryId") { defaultValue = "" },
                            navArgument("categoryName") {}),
                        deepLinks = listOf(
                            navDeepLink {
                                uriPattern = "$deepLinkUri/basic/{categoryName}.html"
                            },
                            navDeepLink { uriPattern = "$deepLinkUri/basic/{categoryName}" })
                    ) { backStackEntry ->
                        val categoryId = backStackEntry.getCategoryId()
                        if (categoryId != null) {
                            val viewModel = getViewModel<BasicGroupsViewModel>(
                                parameters = { parametersOf(categoryId) }
                            )
                            BasicGroupsScreen(onNavigate, viewModel)
                        } else {
                            // open tips screen on invalid deeplink parameters
                            TipsScreen(onNavigate)
                        }
                    }
                    composable(
                        "command?commandId={commandId}&commandName={commandName}",
                        arguments = listOf(
                            navArgument("commandId") { defaultValue = "" },
                            navArgument("commandName") {}),
                        deepLinks = listOf(
                            navDeepLink { uriPattern = "$deepLinkUri/man/{commandName}.html" },
                            navDeepLink { uriPattern = "$deepLinkUri/man/{commandName}" })
                    ) { backStackEntry ->
                        val commandId = backStackEntry.getCommandId()
                        if (commandId != null) {
                            val viewModel = getViewModel<CommandDetailViewModel>(
                                parameters = { parametersOf(commandId) }
                            )
                            CommandDetailScreen(onNavigate, viewModel)
                        } else {
                            // open tips screen on invalid deeplink parameters
                            TipsScreen(onNavigate)
                        }
                    }
                }
            }
        }
    }


}

sealed class Screen(
    val route: String,
    @StringRes val resourceId: Int,
    @DrawableRes val drawableRes: Int
) {
    object Commands : Screen("commands", R.string.commands, R.drawable.ic_search_40dp)
    object Basics : Screen("basics", R.string.basics, R.drawable.ic_puzzle)
    object Tips : Screen("tips", R.string.tips, R.drawable.ic_idea)
}