definition(
        name: 'Garage Door App',
        namespace: 'Petro',
        author: 'Matthew Petro',
        description: 'Combine a relay, siren, and sensors into a complete garage door solution',
        category: 'Convenience',
        iconUrl: '',
        iconX2Url: ''
)

preferences {
    section('Devices') {
        input 'relay', 'capability.switch', title: 'Relay that controls the garage door opener', required: true
        input 'willRelayAutoOpen', 'bool', title: 'Set to true if the relay device is configured to auto open after a delay. Set to false if you want to use the setting below to open the relay after the specified number of milliseconds.', required: true, defaultValue: true
        input 'relayCloseTime', 'number', title: 'Number of milliseconds to keep relay closed (default: 250, range: 100..1000)', required: false, defaultValue: 250, range: '100..1000'
        input 'openContactSensor', 'capability.contactSensor', title: 'Contact/tilt sensor that detects door open state', required: true
        input 'closedContactSensor', 'capability.contactSensor', title: 'Contact/tilt sensor that detects door closed state', required: true
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
    def doorDevice = addChildDevice('Petro', 'Simulated Garage Door Device', deviceNetworkId, null, [label: 'Simulated Garage Door Device'])
    state.deviceNetworkId = deviceNetworkId
    subscribe(doorDevice, 'door', 'garageDoorChangeHandler')
}

def updated() {
    logDebug 'Updated'
    subscribe(openContactSensor, 'contact', 'openSensorHandler')
    subscribe(closedContactSensor, 'contact', 'closedSensorHandler')
}

def uninstalled() {
    logDebug 'Uninstalled'
    unsubscribe(openContactSensor)
    unsubscribe(closedContactSensor)
    deleteChildDevice(getChildDevice(state.deviceNetworkId))
}

def garageDoorChangeHandler(event) {
    logDebug "garageDoorChangeHandler() called: ${event.name} ${event.value}"
    String actualDoorState = actualDoorState()
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

// This method is meant to verify that the door actually opened or closed after the relay was pressed.
// If the door doesn't move because it has been physically blocked or disabled, the contacts
// won't change state and the door device will be left in an opening or closing state.
// If the door device is not open or closed after a certain amount of time, this method
// will sync the device state to the actual door state.
private syncDoorState() {
    logDebug 'syncDoorState()'
    def doorDevice = getChildDevice(state.deviceNetworkId)
    def doorDeviceState = doorDevice.currentValue('door')
    if (!['open', 'closed'].contains(doorDeviceState)) {
        String actualDoorState = actualDoorState()
        if (['open', 'closed'].contains(actualDoorState)) {
            doorDevice.doorChangeHandler(actualDoorState)
        }
    }
}

private String actualDoorState() {
    if (openContactSensor.currentValue('contact') == 'closed') {
        return 'open'
    } else if (closedContactSensor.currentValue('contact') == 'closed') {
        return 'closed'
    } else {
        return 'unknown'
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
        if (debug) logDebug 'Activating alarm device'
        if (alarmSound && alarmStrobe) {
            alarm.both()
        } else if (alarmSound) {
            alarm.siren()
        } else if (alarmStrobe) {
            alarm.strobe()
        }
        pauseExecution(alarmDuration * 1000)
        if (debug) logDebug 'Stopping alarm device'
        alarm.off()
    }
    if (null != tone) {
        if (debug) logDebug 'Playing alert tone'
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