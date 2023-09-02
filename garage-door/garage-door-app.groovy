/**
 * Garage Door App for Hubitat. Inspired by lgkahn's virtual garage door app originally written
 * for SmartThings: https://github.com/lgkahn/hubitat/blob/master/lgkgaragedoor.groovy
 *
 * The app uses one or two contact or tilt sensors to determine the state of the garage door. If only one sensor is
 * used, it should be placed in a location so that it is closed when the door is fully closed. If two sensors are used,
 * one should be situated to detect when the door is fully closed and the other should detect when the door is fully
 * open.
 *
 * The app relies on a relay device to control the physical opener. Using a dry contact relay that is hard-wired to the
 * opener or to a remote control is recommended, but any device that supports switch capability can be used.
 */

definition(
        name: 'Garage Door App',
        namespace: 'MPetro',
        author: 'Matthew Petro',
        description: 'Combine a relay, siren, and sensors into a complete garage door solution',
        category: 'Convenience',
        importUrl: 'https://raw.githubusercontent.com/matthewpetro/hubitat-projects/main/garage-door/garage-door-app.groovy',
        iconUrl: '',
        iconX2Url: ''
)

preferences {
    section('Devices') {
        input 'relay', 'capability.switch', title: 'Relay that controls the garage door opener', required: true
        input 'willRelayAutoOpen', 'bool', title: 'Set to true if the relay device is configured to auto open after a delay. Set to false if you want to use the setting below to open the relay after the specified number of milliseconds.', required: true, defaultValue: true
        input 'relayCloseTime', 'number', title: 'Number of milliseconds to keep relay closed (default: 250, range: 100..1000)', required: false, defaultValue: 250, range: '100..1000'
        input 'closedContactSensor', 'capability.contactSensor', title: 'Contact/tilt sensor that detects door closed state', required: true
        input 'openContactSensor', 'capability.contactSensor', title: 'Contact/tilt sensor that detects door open state', required: false
        input 'doorStateCheckDelay', 'number', title: 'Number of seconds to wait before verifying that door has opened or closed (default: 30, range: 1..60)', required: false, defaultValue: 30, range: '1..60'
    }

    section('Audio alerts - set either an alarm or a tone device') {
        input 'alarm', 'capability.alarm', title: 'Alarm?', required: false
        input 'alarmDuration', 'number', title: 'Number of seconds to sound alarm (default: 10, range: 1..60)?', required: false, defaultValue: 10, range: '1..60'
        input 'alarmSound', 'bool', title: 'Sound alarm?', required: false, defaultValue: true
        input 'alarmStrobe', 'bool', title: 'Flash alarm strobe?', required: false, defaultValue: true
        input 'tone', 'capability.tone', title: 'Tone device?', required: false
    }

    section('Logging') {
        input('debug', 'bool', title: 'Enable debug logging?', required: true, defaultValue: false)
    }
}

def installed() {
    logDebug 'Installed'
    updated()
    def deviceNetworkId = "${app.id}-simulated-garage-door-device"
    def doorDevice = addChildDevice('MPetro', 'Simulated Garage Door Device', deviceNetworkId, null, [label: 'Simulated Garage Door Device'])
    state.deviceNetworkId = deviceNetworkId
    subscribe(doorDevice, 'door', 'garageDoorChangeHandler')
}

def updated() {
    logDebug 'Updated'
    unsubscribe('closedSensorHandler')
    unsubscribe('openSensorHandler')
    subscribe(closedContactSensor, 'contact', 'closedSensorHandler')
    if (null != openContactSensor) {
        subscribe(openContactSensor, 'contact', 'openSensorHandler')
    }
}

def uninstalled() {
    logDebug 'Uninstalled'
    unsubscribe(closedContactSensor)
    if (null != openContactSensor) {
        unsubscribe(openContactSensor)
    }
    deleteChildDevice(getChildDevice(state.deviceNetworkId))
}

def garageDoorChangeHandler(event) {
    logDebug "garageDoorChangeHandler() called: ${event.name} ${event.value}"
    def actualDoorState = actualDoorState()
    logDebug "actualDoorState: ${actualDoorState}"
    if (event.value == 'opening' && actualDoorState == 'closed') {
        pressGarageDoorButton()
        runIn(doorStateCheckDelay, 'syncDoorState')
    } else if (event.value == 'closing' && actualDoorState == 'open') {
        playAudioAlert()
        pressGarageDoorButton()
        runIn(doorStateCheckDelay, 'syncDoorState')
    }
}

// This method is meant to verify that the door actually opened or closed after the relay was pressed
// and sync the door state with the simulated garage door device if necessary.
private syncDoorState() {
    logDebug 'syncDoorState()'
    def doorDevice = getChildDevice(state.deviceNetworkId)
    def doorDeviceState = doorDevice.currentValue('door')
    if (!['open', 'closed'].contains(doorDeviceState)) {
        def actualDoorState = actualDoorState()
        if (['open', 'closed'].contains(actualDoorState)) {
            doorDevice.doorChangeHandler(actualDoorState)
        }
    }
}

private actualDoorState() {
    if (null != openContactSensor) {
        // If a contact sensor is configured to detect the open state
        if (openContactSensor.currentValue('contact') == 'closed') {
            return 'open'
        } else if (closedContactSensor.currentValue('contact') == 'closed') {
            return 'closed'
        } else {
            return 'unknown'
        }
    } else {
        // If only a contact sensor is configured to detect the closed state
        if (closedContactSensor.currentValue('contact') == 'closed') {
            return 'closed'
        } else {
            return 'open'
        }
    }
}

private pressGarageDoorButton() {
    logDebug 'pressGarageDoorButton()'
    relay.on()
    if (!willRelayAutoOpen) {
        pauseExecution(relayCloseTime)
        relay.off()
    }
}

private playAudioAlert() {
    if (null != alarm) {
        logDebug 'Activating alarm device'
        if (alarmSound && alarmStrobe) {
            alarm.both()
        } else if (alarmSound) {
            alarm.siren()
        } else if (alarmStrobe) {
            alarm.strobe()
        }
        pauseExecution(alarmDuration * 1000)
        logDebug 'Stopping alarm device'
        alarm.off()
    }
    if (null != tone) {
        logDebug 'Playing alert tone'
        tone.beep()
    }
}

def openSensorHandler(event) {
    logDebug "openSensorHandler() called: ${event.name} ${event.value}"
    def doorDevice = getChildDevice(state.deviceNetworkId)
    def doorDeviceState = doorDevice.currentValue('door')

    if (event.value == 'open' && doorDeviceState != 'closing') {
        doorDevice.doorChangeHandler('closing')
    } else if (event.value == 'closed') {
        doorDevice.doorChangeHandler('open')
    }
}

def closedSensorHandler(event) {
    logDebug "closedSensorHandler() called: ${event.name} ${event.value}"
    def doorDevice = getChildDevice(state.deviceNetworkId)
    def doorDeviceState = doorDevice.currentValue('door')

    if (event.value == 'open' && doorDeviceState != 'opening') {
        doorDevice.doorChangeHandler('opening')
    } else if (event.value == 'closed') {
        doorDevice.doorChangeHandler('closed')
    }
}

private void logDebug(message) {
    if (debug) log.debug message
}