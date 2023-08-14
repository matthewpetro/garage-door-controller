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

def installed() {
   log.debug "installed()"
}

def updated() {
   log.debug "updated()"
}

def configure() {
    log.debug "configure()"
}

def open() {
    log.debug "open()"
    sendEvent(name: "door", value: "opening")
}

def close() {
    log.debug "close()"
    sendEvent(name: "door", value: "closing")
}

def on() {
    open()
}

def off() {
    close()
}

def doorChangeHandler(newValue) {
    log.debug "doorChangeHandler ${newValue}"
    sendEvent(name: "door", value: newValue)
}
