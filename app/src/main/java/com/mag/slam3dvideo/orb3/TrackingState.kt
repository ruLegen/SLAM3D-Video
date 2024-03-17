package com.mag.slam3dvideo.orb3

enum class TrackingState(val state: Int) {
    SYSTEM_NOT_READY(-1),
    NO_IMAGES_YET(0),
    NOT_INITIALIZED(1),
    OK(2),
    RECENTLY_LOST(3),
    LOST(4),
    OK_KLT(5),
    UNKNOWN(Int.MAX_VALUE);

    companion object {
        fun fromInt(value: Int) = entries.firstOrNull { it.state == value } ?: UNKNOWN;
    }
}