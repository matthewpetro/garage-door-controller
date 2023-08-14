metadata {
    definition(name: "Simulated Garage Door Device", namespace: "Petro", author: "Matthew Petro") {
        capability "Actuator"
        capability "Configuration"
        capability "Door Control"
        capability "Garage Door Control"
        capability "Sensor"
        capability "Switch"
    }
}

preferences {
    section("Logging") {
        input("debug", "bool", title: "Enable logging?", required: true, defaultValue: false)
    }
}

def installed() {
    logDebug "installed()"
}

def updated() {
    logDebug "updated()"
}

def configure() {
    logDebug "configure()"
}

def open() {
    logDebug "open()"
    sendEvent(name: "door", value: "opening")
}

def close() {
    logDebug "close()"
    sendEvent(name: "door", value: "closing")
}

def on() {
    open()
}

def off() {
    close()
}

def doorChangeHandler(newValue) {
    logDebug "doorChangeHandler ${newValue}"
    sendEvent(name: "door", value: newValue)
}

private logDebug(message) {
    if (debug) log.debug message
}