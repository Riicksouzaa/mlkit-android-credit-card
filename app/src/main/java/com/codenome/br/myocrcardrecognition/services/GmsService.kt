package com.codenome.br.myocrcardrecognition.services

import android.content.Context
import com.google.android.gms.common.GoogleApiAvailability

enum class MobileService {
    GMS
}

internal typealias GMSConnectionResult = com.google.android.gms.common.ConnectionResult

fun Context.getMobileService(): MobileService = when (GMSConnectionResult.SUCCESS) {
    GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) -> MobileService.GMS
    else -> error("unsuported service")
}