# Skipii changes

What each release brings, in a line or two per change, written for the people using the app.
`scripts/publish-apk.sh` posts a version's section as its GitHub release notes, and the app's
What's new sheet (tap Update on Home) shows them before installing.

Add every change someone would notice under **Unreleased**, under `### New`, `### Improved` or
`### Miscellaneous`. Publishing renames Unreleased to the version. Leave out under-the-hood work unless it
changes something people see or feel (faster, fewer errors); then say that.

## Fork changes (unreleased)
- A short's linked video opens from the description's bottom edge — the one the author set for it, YouTube's blue-circle row: another short plays in the Shorts pager, a full-length video in the player. With nothing linked, the author's channel row stays.
- The 保存したショート動画 (Saved shorts) playlist opens now: YouTube answered it as shorts beside an empty video list, which showed as a blank page — the shorts take the list's place, read as a wall of 9:16 tiles like the channel's Shorts tab, and Play runs them in the Shorts pager.
- The rail's save button opens the full save sheet again (後で見る or any of the account's own playlists). YouTube's 保存したショート動画 playlist is read-only over InnerTube — its header says it can't be edited and every add is refused — so the fork can show and play it but not save into it; the direct save and its 「保存先を毎回確認」 toggle are gone.
- Add 保存したショート動画 (Saved shorts): when the account has the playlist it is pinned in Library, and a playlist of nothing but shorts plays in the Shorts player instead of the video one (Settings › Shorts decides, on by default). Signing out or switching accounts forgets the learned playlist.
- History shows Shorts in their own shelf beside the videos, and tapping one plays the history's shorts in the Shorts player.
- A short's description ends with its author: the channel's picture and name open the channel on its Videos tab, pinned at the description's bottom edge whether the 概要欄 is open or not.
- Add Shorts picture-in-picture: leaving the app while a short plays floats it in a window that carries the short's own 9:16, the rail's PiP button enters it by hand, and closing the window pauses the short (a toggle in Settings › Shorts, off by default).
- Add Shorts autoplay: a short that runs out starts the next one by itself (a toggle in Settings › Shorts, off by default).
- The fork's row in Settings › About shows a build number with the date (2026.11.18 (4)), and the maintainer's name in the line opens their GitHub profile — the row itself still opens the repository.
- Add chapter marks in the player: notches on the progress bar where the chapters change, the current chapter's name beside the time (tapping it opens the chapter list), and the description's timestamps seek to their moment when tapped.
- The player's related videos follow the app's language: their titles, channels, view counts and ages come from InnerTube in the content language (the request that already carries the description, now shared) instead of NewPipeExtractor's English watch page.
- Bring in upstream's Yuno 1.1.1: the channel page's tabs (Home, Videos, Shorts, Live, Playlists, Posts, About), home recommendation categories, release-style playlist pages, Flow-style search with filter chips and anchored menus, livestreams with live chat, sharper thumbnails, letterboxed widescreen videos and Shorts, a player that opens from the tapped thumbnail with a morph and returns to the same spot, search results staying behind the player, the status bar's blur following the video, and channel/playlist colours as an opt-in (Settings › Appearance).
- The audio-only mode is gone with the merge: upstream removed it in 1.1.0, so its switch, stage and kept preference are out of the fork too.
- The 69 strings the release added are translated into Japanese, so the new screens speak the app's language like the rest.
- Add YouTube in the app's language: a content-language setting that makes YouTube answer in the language the app runs in, so feeds, chips, view counts, ages and descriptions come back localized.
- Add multiple Google accounts and brand channels: several Google accounts signed in side by side with quick switching, each acting as one of the brand channels it manages.
- Fix the account photo while acting as a brand channel: the top bar, the account row and the comment composer show the acting channel's identity, kept across restarts while the delegation lasts.
- Add the Enhancements page: Return YouTube Dislike, DeArrow, the player's mute, loop and copy buttons, tracking-parameter-free share links, and a per-codec decoder policy on one settings page.
- Add an adjustable tab bar: reorder, hide and show the bottom bar's tabs in Settings › Appearance › Tab bar.
- Add empty states: empty feeds show an empty state with a retry, and a signed-in Home with nothing of its own falls back to the guest recommendations for the session.
- Add a self-recovering bot check: when YouTube's "confirm you're not a bot" refuses the streams, Yuno keeps waiting it out in the background and retries when the network changes, then opens the video by itself — and a video playing its 360p fallback swaps to the real streams.
- Add signed-in extraction: while signed in, the video's player requests ride the Google session the way yt-dlp uses cookies, so YouTube reads them as signed-in ones and the bot check rarely comes up (a toggle in Settings › Enhancements, on by default).
- Add comment translation: a Translate button under every comment turns it into the app's language, and the original is one tap away.
- Show the fork's own version and maintainer in Settings › About.
- The player's Save and Playlist pills follow the app's language instead of staying English.
- Add skip silence: long quiet stretches in a video are fast-forwarded while it plays (a toggle in Settings › Playback, off by default).
- The Shorts screen's chrome (Subscribe, Go to channel, Back, Comments, Share, the error notes) follows the app's language instead of staying English.
- Add Shorts at their own ratio: a short keeps its shape with black bars instead of being blown up to fill the screen (a toggle in Settings › Shorts).
- Add quality per network: separate video-height ceilings for Wi-Fi and for mobile data, each overriding the player's own pick while on that connection (Settings › Playback).
- The fullscreen brightness and volume side sliders can be turned off (Settings › Playback).
- Add Shorts like, dislike and comment counts on the rail, and make the dislike button act as YouTube's "not interested": a real dislike plus the channel's Shorts stop being offered, skipping to the next short.
- Add the short's description behind a 「もっと見る」 (More) under the title, folding open and closed in place.
- Shorts' double-tap like can be turned off, and holding a short plays it at 2× until the finger lifts (toggles in Settings › Shorts).
- The Enhancements page is grouped into five sections: extraction, community APIs, the player's own buttons, sharing, and the decoder policy.
- The video's scrub gestures are choosable: a tap on the progress bar jumps there, holding the video for 2× can be kept or turned off, and a horizontal slide on the video scrubs from where play is (toggles in Settings › Playback; the slide is off by default).
- Add downloading: a video's long-press menu and the quality pickers open a sheet that saves the combined MP4 (YouTube's combined streams top out at 360p) or the audio's M4A through Android's Download Manager, and an external-downloader row hands the cleaned link to an app like Seal for full quality.
- Every screen now speaks the app's language: the search page, the long-press menu, the player's action notes and errors, the SponsorBlock pills, the quality/speed/chapters sheets, the queue, the account sheet's settings rows, the top bar's accessibility labels, and the whole onboarding follow Japanese instead of staying English (the ja tables also gained the two strings they were missing).

## Unreleased

## 1.1.1

![Yuno](https://github.com/danielkanan/Yuno/raw/main/docs/banner.png?v=2)

### New
- Tapping a video opens the full player from that thumbnail. Minimising puts it in the bar, and tapping that same video opens it from the bar.

### Improved
- Going back from a video, Short, channel, or playlist returns you to the same spot. Search stays open behind the player.
- The blur behind the status bar follows the video you just switched to.

### Miscellaneous
- Channel and playlist pages stay plain until you turn on Channel & playlist colors in Settings › Appearance.

## 1.1.0

![Yuno](https://github.com/danielkanan/Yuno/raw/main/docs/banner.png?v=2)

### New
- Channel pages take their colour from the profile picture. Tabs swipe between Home, Videos, Shorts, Live, Playlists and the rest, and Posts and About are their own pages.
- Home has recommendation categories along the top. Each one keeps its own list.
- A playlist page is the colour of its cover, with Play and Shuffle, and the videos in a grid.
- Search has type chips under the bar, and a filter menu for duration, date and sort. A channel, a Shorts row, a video shelf and posts stay as their own blocks.
- Livestreams play, with the watching count and live chat. A live thumbnail says LIVE.
- Thumbnails, Shorts and post pictures are sharper.

### Improved
- A video wider than usual keeps the normal player, with black bars. A Short fits on the screen with black bars instead of being enlarged to fill it.
- The Update button on Home sits clear of search and your picture.

### Miscellaneous
- Audio only is gone.

## 1.0.0

![Yuno](https://github.com/danielkanan/Yuno/raw/main/docs/banner.png?v=2)

### First release of Yuno
- Home, Subscriptions, Shorts, Search, and Library
- Recommendations when you're signed out, your own feed when you sign in
- Liquid glass, a mini player, captions, and SponsorBlock

## 0.5.109

### New
- **Crash report** in **Settings › About** (#55). If Skipii closes by itself, open it again, tap Crash report and paste what it shares into a GitHub issue. It says what went wrong and how Android closed the app, with your phone's model and Android version, and nothing about your account or what you watched. Settings › About says "Crash report saved" after a crash.

## 0.5.108

### Improved

- Flex mode on a Galaxy Z Flip: the controls under the fold no longer squeeze into slivers. Play and skip are bigger, and the other buttons get a row of their own with their names in full.
- Flex mode: buttons that are on (captions, a changed speed, audio only) are now a soft tint instead of a bright fill, so play stays the one that stands out.

### Fixed

- Tablets held upright now show videos two to a row, instead of one huge card across the screen.

## 0.5.106

### New
- Channel pages look like YouTube's (#53, thanks @usa2525-code). Tabs for **Home**, **Videos**, **Shorts** and **Playlists** sit under the profile and stay at the top as you scroll. Home shows the channel's featured video and its own rows (latest uploads, playlists, featured channels), each with See all. Videos can be sorted Latest, Popular or Oldest.
- Tapping a short on a channel plays it in Shorts, and swiping goes through that channel's other shorts. Back takes you to the channel again, and your Shorts feed is where you left it.
- A channel's playlists open even when you're signed out.
- Drag along a short's progress bar to go anywhere in it (#44, thanks @ganapathi-subramanian). The bar thickens under your finger and the time shows above it; a tap on the bar jumps there.
- **Not interested** and **Don't recommend channel** in the long-press menu of the videos YouTube recommends you when you're signed in (#16, thanks @RaulKong878). The video, or every video from that channel, leaves your feeds at once, and YouTube learns from it.
- Choose how far a double tap and the back / forward buttons jump, from 5 to 30 seconds in steps of 5, in **Settings › Playback** (#36, thanks @jm11x). The buttons show the number you picked.
- Music plays at normal speed even when you keep a speed for every video, and the next video that isn't music is back at your speed (#35, thanks @risingsword). You can turn this off in **Settings › Playback**.
- **Keep chapters open** in **Settings › Playback** (#46, thanks @Suhareb): picking a chapter leaves the list open, so you can jump from one chapter to the next.
- **Picture-in-picture when you leave** in **Settings › Playback** (#42, thanks @ganapathi-subramanian): go home while a video plays and it keeps playing in a small window. Off unless you turn it on.

### Improved
- When YouTube asks to confirm you're not a bot, the player now says what usually fixes it (turning airplane mode on and back off) and has an **Open in YouTube** button that opens the video in YouTube's app from where you were, next to Try again.
- Subscriptions opens faster. Your subscriptions are loaded in the background once Home is in, and the row of channels at the top is there right away instead of appearing a few seconds after the videos.
- Subscriptions shows your channels' videos newest first, in the order they went up. YouTube's own "Most relevant" picks no longer sit at the top (#50).
- In Settings, the option picked in a row of choices (like Player controls or Skip back and forward) is now filled in with your theme's colour, so it's easy to tell which one is on.
- The progress bar in Shorts sits just above the buttons, under the title, where it's easier to see (#48, thanks @Suhareb).

### Fixed
- On a Galaxy Z Flip's cover screen, videos no longer get stuck in full screen. The player opens normally there, and the full screen button goes in and out again.
- Tapping a channel at the top of Subscriptions now opens it (#37, thanks @Rooknee56, and #32, thanks @ganapathi-subramanian).

## 0.5.99

### New
- Double tap a short to like it, as on YouTube (#47, thanks @Suhareb). A thumbs up pops out just above where you tapped, clear of your finger, with a burst of confetti; keep tapping for more. A double tap never takes a like away, and a single tap still pauses.

### Fixed
- Liking a short (double tap or the like button) works the moment it comes on screen. It used to do nothing for about two seconds after each swipe.
- Closing the comments brought the player's controls back with their top half cut off for a moment. They now come back whole.

## 0.5.94

### Fixed
- On near-square phones like the Unihertz Titan 2 Elite and other keyboard phones, fullscreen no longer turns the screen sideways. The video fills the screen the way you're holding it, and you can still turn the phone sideways for landscape.

## 0.5.93

### New
- Tablets and foldables get a layout of their own. On a wide screen the tabs move to a rail down the left, feeds become grids, and the mini-player waits in the corner.
- Watching on a tablet: the video, its details and the comments scroll together on the left, and More videos stays in its own column on the right, with filters for the channel's own videos and the rest.
- On an open Galaxy Fold or Pixel Fold the video runs across the top, the details and comments sit on the left page, and More videos on the right. The playback controls stay centred, as on a phone.
- Stand an open foldable half open on a table and the video takes the top half while the controls lie on the bottom half on pure black, with a strip of preview frames you drag across to jump anywhere.
- Shorts on a big screen play as big as the screen allows, never cropped, with the title and buttons beside them, and the comments open beside the short. On Home they're a single card, not a shelf.
- Settings on a big screen shows its categories on the left and the one you pick on the right.
- Home shows YouTube's categories (All, Music, Gaming and more) at the top, on phones too (#35, thanks @risingsword).
- Zoom to fill in fullscreen, as in YouTube's app: pinch out on the video to fill the whole screen (the edges are cropped), pinch in to see it whole again. A pinch counts for that video only. To have every video fill the screen, turn on Settings › Playback › Zoom to fill in fullscreen (#41, #43, thanks @ganapathi-subramanian and @risingsword).

### Improved
- Tablets and open foldables are no longer held in portrait: turn them any way you like.

### Fixed
- Live streams no longer show up in Home, Subscriptions, channels or playlists. Skipii can't play them yet, so they only led to an error (search already left them out).
- Foldables: folding or unfolding keeps you where you were. The same video or short carries on playing, on the same page, instead of starting over on Home.
- Galaxy Z Fold: a video no longer gets stuck sideways in fullscreen on the cover screen after you fold the phone. The screen you switch to follows how you're holding it.
- Coming back to the app while a video plays in the background now loads that video's comments again.

## 0.5.84

### New
- A real Settings page. Tap your profile picture (or the person icon) at the top of Home, Subscriptions or Library. Search finds any setting, and tapping a result takes you straight to it. Audio only, SponsorBlock, Shorts and Keep speed switch on and off right from the top of the page. Everything else sits in categories: Playback, Captions, SponsorBlock, Shorts, Appearance, Language, Notifications, About and your account.
- Notifications lists every channel whose bell is on, so you can turn bells off in one place.
- About shows your version and checks for an update when you tap it.
- Say hi to the developer from the bottom of Settings: Instagram, Threads, X, or an email.

## 0.5.80

### New
- Skipii in your language: Spanish, Portuguese, French, German, Italian, Indonesian, Turkish, Vietnamese, Russian, Arabic, Hindi and Japanese. It follows your phone's language; pick another in the new Language card at the top of the account sheet (on Android 13 and later also in Settings › Apps › Skipii › Language). Views, subscriber counts and "3 days ago" are written in your language too. Videos, titles and comments stay as their creators wrote them.

## 0.5.78

### Improved
- Captions show like YouTube's: two whole lines at a time, switching straight to the next two as they're said, instead of one word at a time.
- The captions button turns captions on with one press; the list it opens is there to pick another language.
- The two caption lines sit together in one box, with no gap between them.

## 0.5.75

### New
- Captions. Every track a video has (the creator's, other languages, auto-generated) in ⋮ › Captions in the player's bar, or the CC button on the video and in fullscreen. Auto-generated captions appear word by word as they're said.
- Captions in any language, translated on your phone when the video doesn't have yours (a one-time download of about 30 MB per language, then it works offline).
- Captions turn themselves on when a video isn't in your phone's language. Change that, their look (Skipii, Classic, Outline or Android's own caption settings) and their size in the new Captions card in the account sheet.
- Transcript: every line of a video's captions, under the video like the comments. The line being said lights up as it's said, tap any line to jump there, and search finds a word.
- The bell. Once you're subscribed, a bell sits next to Subscribed (on the video, the channel page and the Shorts channel card). Turn it on and Skipii sends a notification for every new video from that channel; tap one to play it. It's your YouTube account's own bell ("All"), so channels you've already belled in the YouTube app show up within a day, and turning it on here turns it on there.

## 0.5.74

### New
- Hold a finger on the right side of the video to play at 2×, in portrait and fullscreen. Let go and it goes back to the speed it had (a paused video plays while held, then pauses again) (#11, #26, #32, #36, thanks @usa2525-code, @hpqs2024-sys, @ganapathi-subramanian and @jm11x).

### Improved
- Save to playlist opens to the same height as the comments, just under the video, in one smooth motion, and scrolls inside.
- The player's Save (Watch later) button is gone: Watch later is the first row of Playlist.
- Picking a playlist animates (the check pops in), "Saved to …" shows right over the list, and the video above stays bright while it's open.

## 0.5.69

### Fixed
- In fullscreen, Speed opens a small list of speeds under its button, like Quality, instead of a sheet over the whole video. "Every video" at the bottom keeps the speed for every video (#31, thanks @nsk-47).
- Swiping the video down to the mini-player with the comments open now takes the comments along and closes them, instead of leaving them open over the mini-player (#33, thanks @BaooRaami).

## 0.5.68

### New
- Tapping Update now shows what's new in the update first, with the Update button under the list (#30, thanks @usa2525-code).
- Player controls can sit on the video instead of the playback pill (account sheet → Player controls). On the video, the buttons look like the pill's with a round play button, and the video's bottom edge is the progress bar: drag its dot to scrub, with a preview of where you'll land (#28, thanks @BaooRaami).

### Improved
- Fullscreen's controls match the new look: round play button, pill-style buttons, and a slimmer progress bar with gaps between chapters.
- Thumbnails in the feeds are wider, closer to the screen's edge (#27, thanks @nsk-47).
- On narrow phones the channel name gets its own line, so views and upload time are no longer cut off (#27, thanks @BaooRaami).

### Fixed
- In fullscreen, Quality opens a small list under its button instead of a sheet over the whole video (#25, thanks @nsk-47).
