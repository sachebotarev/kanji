package ru.mobui.kanji

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeepLinkNavigationTest {
    @Test
    fun packageManagerResolvesHubDeepLinksToMainActivity() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        listOf(
                AppDeepLinks.HOME,
                AppDeepLinks.TRAINING_PICK,
                AppDeepLinks.SETTINGS,
                AppDeepLinks.CHECKER_SETTINGS,
                AppDeepLinks.CATALOG,
                AppDeepLinks.REFERENCES,
                AppDeepLinks.DICTIONARY,
                AppDeepLinks.RECIPES,
                "${AppDeepLinks.RECIPES}?openCreate=true",
            )
            .forEach { uri ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                val resolved =
                    context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                assertNotNull("No activity for $uri", resolved)
                assertTrue(
                    "Expected MainActivity for $uri, got ${resolved!!.activityInfo.name}",
                    resolved.activityInfo.name.endsWith("MainActivity"),
                )
                assertTrue(
                    "Expected package ${context.packageName} for $uri",
                    resolved.activityInfo.packageName == context.packageName,
                )
            }
    }
}
