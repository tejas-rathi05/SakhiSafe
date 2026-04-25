package com.heysafe.app.wear

object WearMessages {
    const val PATH_VITALS = "/vitals/sample"
    const val PATH_ALERT_CONFIRMED = "/alert/confirmed"
    const val PATH_ALERT_CANCELED = "/alert/canceled"

    const val KEY_HR = "hr"
    const val KEY_BASELINE = "baseline"
    const val KEY_MOTION = "motion"
    const val KEY_TS = "ts"
    const val KEY_TRIGGER_SOURCE = "triggerSource"
    const val KEY_HR_WINDOW = "hrWindow"
    const val KEY_MOTION_WINDOW = "motionWindow"

    const val SOURCE_HEURISTIC = "heuristic"
    const val SOURCE_ML = "ml"
    const val SOURCE_BOTH = "both"
    const val SOURCE_MANUAL = "manual"
}
