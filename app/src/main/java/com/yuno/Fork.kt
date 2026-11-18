package com.yuno

/**
 * The fork's own identity, shown in Settings › About ([Settings] AboutBody) — separate from
 * Yuno's version row, which stays upstream's. The version is the fork's state date; bump it
 * when a feature lands, and keep [AUTHOR]/[PROFILE]/[URL] honest with the repository's owner.
 */
object Fork {
    const val VERSION = "2026.11.18"

    /**
     * Which build of the fork this is, bumped by hand with every apk that leaves the machine:
     * 2026.11.16 = 1, 2026.11.17 = 2, the 1.1.1 merge = 3, this feature build = 4. It rides
     * along the version (same-day rebuilds differ, as YouTube's own versionCode does).
     */
    const val BUILD = 4

    const val AUTHOR = "Ren9618"

    /** The maintainer's GitHub profile — where the About page's author link goes. */
    const val PROFILE = "https://github.com/Ren9618"

    const val URL = "https://github.com/Ren9618/Yuno"

    /** The version as Settings shows it: the state date with the build number. */
    val label: String get() = "$VERSION ($BUILD)"
}
