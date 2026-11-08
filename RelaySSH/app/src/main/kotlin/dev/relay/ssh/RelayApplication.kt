package dev.relay.ssh

import app.one.secondvpnlite.SecondVPN

// Compatibility adapter: retain existing application initialization during Kotlin migration.
class RelayApplication : SecondVPN()
