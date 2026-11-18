package com.yuno

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import org.schabi.newpipe.extractor.NewPipe

class YunoApplication : Application(), ImageLoaderFactory {

    /** The Shorts feed, players and bank: one per process, outliving any window. */
    val shorts by lazy { ShortsState(this, kotlinx.coroutines.MainScope()) }

    override fun onCreate() {
        super.onCreate()
        // Before anything that could crash, so its stack trace is kept (Settings › About › Crash report).
        CrashLog.install(this)
        // First: everything below may already word something (str()).
        AppLanguage.init(this)
        // Which language YouTube itself answers in (the app's unless picked).
        ContentLanguage.init(this)
        NewPipe.init(NewPipeDownloader.instance)
        // The iOS client only adds HLS livestream formats; Yuno plays VOD, so skip that request.
        org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor.setFetchIosClient(false)
        ThemeSettings.init(this)
        ChannelColorSetting.init(this)
        LiquidGlassSetting.init(this)
        Onboarding.init(this)
        QualityPreference.init(this)
        PlaybackSpeed.init(this)
        CodecSettings.init(this)
        PlayerControls.init(this)
        FullscreenZoom.init(this)
        SideSliders.init(this)
        ScrubGestures.init(this)
        NetworkQuality.init(this)
        SeekStep.init(this)
        ChaptersStayOpen.init(this)
        AutoPip.init(this)
        SkipSilence.init(this)
        CaptionSettings.init(this)
        CaptionTranslator.refresh()
        SearchHistory.init(this)
        ResumeStore.init(this)
        SavedSession.init(this)
        Account.init(this)
        Bells.init(this)
        NewVideos.schedule(this)
        SponsorBlock.init(this)
        Enhancers.init(this)
        ReturnYouTubeDislike.init(this)
        DeArrow.init(this)
        ShortsSetting.init(this)
        ShortsRatio.init(this)
        ShortsGestures.init(this)
        ShortsAutoplay.init(this)
        ShortsPip.init(this)
        PlaylistShortsPlayer.init(this)
        ShortsSaveAsk.init(this)
        ShortsNotInterested.init(this)
        SavedShortsPlaylist.init(this)
        NavTabs.init(this)
        ShortsBank.init(this)
        AppUpdate.init(this)
        // Stream URLs are bound to the network they were fetched on: forget them on a switch.
        // A video waiting out a bot check retries right away on the fresh address, too —
        // the check rides on the address, which is what the airplane-mode hint is about.
        getSystemService(android.net.ConnectivityManager::class.java)?.registerDefaultNetworkCallback(
            object : android.net.ConnectivityManager.NetworkCallback() {
                private var last: android.net.Network? = null
                override fun onAvailable(network: android.net.Network) {
                    if (last != null && last != network) {
                        StreamCache.invalidateAll()
                        BotCheckRecovery.networkChanged()
                    }
                    last = network
                }
            },
        )
    }

    // Thumbnails are in every list and the player; a short crossfade replaces pop-in.
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .crossfade(180)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
        .build()
}