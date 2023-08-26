metadata {
    definition(name: 'Simulated Garage Door Device', namespace: 'Petro', author: 'Matthew Petro') {
        capability 'Actuator'
        capability 'Configuration'
        capability 'Door Control'
        capability 'Garage Door Control'
        capability 'Sensor'
        capability 'Switch'
    }
}

preferences {
    section('Logging') {
        input('debug', 'bool', title: 'Enable debug logging?', required: true, defaultValue: false)
    }
}

def installed() {
    logDebug 'Installed'
}

def updated() {
    logDebug 'Updated'
}

def configure() {
    logDebug 'Configured'
}

def open() {
    logDebug 'open()'
    if (device.currentValue('door') != 'open') {
        sendEvent(name: 'door', value: 'opening', isStateChange: true)
    }
}

def close() {
    logDebug 'close()'
    if (device.currentValue('door') != 'closed') {
        sendEvent(name: 'door', value: 'closing', isStateChange: true)
    }
}

def on() {
    open()
}

def off() {
    close()
}

def doorChangeHandler(newValue) {
    logDebug "doorChangeHandler() called: ${newValue}"
    sendEvent(name: 'door', value: newValue)
    if (newValue == 'open') {
        sendEvent(name: 'switch', value: 'on')
    } else if (newValue == 'closed') {
        sendEvent(name: 'switch', value: 'off')
    }
}

private void logDebug(String message) {
    if (debug) log.debug message
}

private void logDebug(GString message) {
    if (debug) log.debug message
}