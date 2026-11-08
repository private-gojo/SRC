package app.one.secondvpnlite

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Kotlin-owned AdMob loading and interstitial display flow.
 * MainActivity keeps Java-compatible entry points during migration.
 */
object RelayMainAds {
    @JvmStatic
    fun setup(context: Context, bannerAd: AdView) {
        // Preserve the original banner configuration and request flow.
        val configuredAdView = AdView(context)
        configuredAdView.setAdSize(AdSize.BANNER)
        val bannerAdUnitId = if (BuildConfig.DEBUG) {
            "ca-app-pub-3940256099942544/6300978111"
        } else {
            "ca-app-pub-2955918284006466/5236078879"
        }
        configuredAdView.setAdUnitId(bannerAdUnitId)

        val adRequest = AdRequest.Builder().build()
        bannerAd.loadAd(adRequest)

        val interstitialAdUnitId = if (BuildConfig.DEBUG) {
            "ca-app-pub-3940256099942544/1033173712"
        } else {
            "ca-app-pub-2955918284006466/2356432963"
        }

        InterstitialAd.load(
            context,
            interstitialAdUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    MainActivity.mInterstitialAd = interstitialAd
                    Log.i("MainActivity", "onAdLoaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.i("MainActivity", loadAdError.message)
                    MainActivity.mInterstitialAd = null
                }
            }
        )
    }

    @JvmStatic
    fun loadInterstitialIfNeeded(
        activity: Activity,
        context: Context,
        preferences: SharedPreferences,
        bannerAd: AdView
    ) {
        if (!SecondVPN.getLastA()) {
            if (!MainActivity.isShowing) {
                activity.runOnUiThread {
                    if (MainActivity.mInterstitialAd == null) {
                        MainActivity.delay = 6000
                        setup(context, bannerAd)
                    }

                    try {
                        MainActivity.isShowing = true

                        Handler().postDelayed({
                            val interstitialAd = MainActivity.mInterstitialAd
                            if (interstitialAd != null) {
                                interstitialAd.show(activity)
                                preferences.edit().putBoolean("LAST_A", true).apply()
                                MainActivity.isShowing = false
                            } else {
                                MainActivity.isShowing = false
                                Log.d("TAG", "The interstitial ad wasn't ready yet.")
                            }
                        }, MainActivity.delay.toLong())
                    } catch (error: Exception) {
                        error.printStackTrace()
                    }
                }
            }
        }
    }
}
