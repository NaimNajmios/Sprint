package com.najmi.sprint.core.domain.util

object PackageDisplayName {

    private val PACKAGE_TO_DISPLAY_NAME = mapOf(
        "com.facebook.katana" to "Facebook",
        "com.facebook.orca" to "Messenger",
        "com.facebook.lite" to "Facebook Lite",
        "com.facebook.mlite" to "Messenger Lite",
        "com.instagram.android" to "Instagram",
        "com.twitter.android" to "X",
        "com.whatsapp" to "WhatsApp",
        "com.whatsapp.w4b" to "WhatsApp Business",
        "com.snapchat.android" to "Snapchat",
        "com.zhiliaoapp.musically" to "TikTok",
        "com.ss.android.ugc.trill" to "TikTok",
        "com.google.android.youtube" to "YouTube",
        "com.google.android.apps.maps" to "Google Maps",
        "com.google.android.gm" to "Gmail",
        "com.google.android.apps.docs" to "Google Docs",
        "com.google.android.calendar" to "Google Calendar",
        "com.google.android.apps.photos" to "Google Photos",
        "com.google.android.keep" to "Google Keep",
        "com.android.chrome" to "Chrome",
        "com.google.android.apps.messaging" to "Messages",
        "com.google.android.dialer" to "Phone",
        "com.google.android.contacts" to "Contacts",
        "org.telegram.messenger" to "Telegram",
        "com.discord" to "Discord",
        "com.slack" to "Slack",
        "com.microsoft.teams" to "Microsoft Teams",
        "com.microsoft.office.outlook" to "Outlook",
        "com.skype.raider" to "Skype",
        "com.spotify.music" to "Spotify",
        "com.netflix.mediaclient" to "Netflix",
        "com.primevideo" to "Prime Video",
        "com.hulu.plus" to "Hulu",
        "com.disney.disneyplus" to "Disney+",
        "com.pinterest" to "Pinterest",
        "com.linkedin.android" to "LinkedIn",
        "com.reddit.frontpage" to "Reddit",
        "com.ubercab" to "Uber",
        "com.lyft" to "Lyft",
        "com.airbnb.android" to "Airbnb",
        "com.booking" to "Booking.com",
        "com.dropbox.android" to "Dropbox",
        "com.android.vending" to "Google Play Store",
        "com.google.android.apps.nbu.files" to "Files by Google",
        "com.google.android.apps.dynamite" to "Google Docs",
        "com.google.android.apps.classroom" to "Google Classroom",
        "com.google.android.apps.meetings" to "Google Meet",
        "com.google.android.apps.tasks" to "Google Tasks",
        "com.google.android.gm" to "Gmail",
    )

    fun forPackage(packageName: String?): String? {
        if (packageName == null) return null
        return PACKAGE_TO_DISPLAY_NAME[packageName]
    }

    fun simplify(packageName: String?): String {
        if (packageName == null) return "Unknown"

        val lookup = forPackage(packageName)
        if (lookup != null) return lookup

        val parts = packageName.split(".")
        return parts.lastOrNull()?.replaceFirstChar { it.uppercase() } ?: packageName
    }
}
