/**
 * Simulated garage door device for Hubitat. Inspired by lgkahn's simulated garage door handler originally written
 * for SmartThings: https://github.com/lgkahn/hubitat/blob/master/simulatedgaragedoorv2.groovy
 *
 * This device driver is intended to be used with the Garage Door App. The driver implements a simulated garage
 * door that can be controlled via Hubitat and anything that can interface with Hubitat (Alexa, Apple HomeKit, etc).
 * The driver also has switch capability so the door can be controlled via simple on and off commands.
 *
 * The code for the driver needs to be present in the Hubitat system, but the actual device instance will be
 * created by the Garage Door App when it is installed.
 */

metadata {
    definition(name: 'Simulated Garage Door Device', namespace: 'MPetro', author: 'Matthew Petro') {
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

private void logDebug(message) {
    if (debug) log.debug message
}
