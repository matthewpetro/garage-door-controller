# Garage Door App
A garage door controller app and driver for Hubitat. Inspired by [lgkahn's virtual garage door app](https://github.com/lgkahn/hubitat/blob/master/lgkgaragedoor.groovy) written for SmartThings. The app ties one or two contact/tilt sensors, a switch/relay, and an optional alarm into a complete garage door control solution.

## Sensors 
The app uses one or two contact/tilt sensors to determine the state of the garage door. It will update the garage door device in Hubitat as needed to keep it in sync with the state of the physical garage door.

### If one sensor is used
The sensor should be placed in a location so that it reports closed when the garage door is fully closed. When the sensor reports an open status, the app will wait a configurable number of seconds. If the sensor still reports open after the waiting period, the app will change the garage door device to `open` state.

### If two sensors are used
One sensor should be situated to detect when the door is fully closed and the other should detect when the door is fully open. The app will monitor both of them and change the garage door device's state to `open` or `closed` as appropriate.

## Relay/switch
The app relies on a relay or switch device to control the physical opener. Using a dry contact relay that is hard-wired to the opener or to a remote control is recommended, but any device that supports switch capability can be used. [Garadget](http://garadget.com) has good options for wiring an opener to a relay, such as the [wireless dry contact adapter](https://www.garadget.com/product/security-2-0-wireless-dry-contact-adapter/). 

## Siren
A siren or tone generator can also be used with the app for safety purposes. If a siren or tone generator is configured, the app will sound an alert before closing the garage door.

## Device driver
The app will create a garage door device in Hubitat. The device driver for this implements both garage door control and switch capabilities so the garage door can be controlled through voice assistants (Alexa, Siri) that understand garage doors or through other interfaces that don't. The app will keep the Hubitat device states in sync with the actual state of the physical garage door and the app will respond to switch commands sent to the device. Sending a switch command of `on` will be the same as sending an `open` command and sending an `off` command will be the same as sending a `close` command.