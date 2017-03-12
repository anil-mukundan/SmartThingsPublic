/**
 *  Left Open Warning For Door With Screen
 *
 *  Copyright 2017 Anil Mukundan
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Left Open Warning For Door With Screen",
    namespace: "anil-mukundan",
    author: "Anil Mukundan",
    description: "Special handling for doors with screens",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Door and Screen") {
        input(name: "door", type: "capability.contactSensor", title: "Monitor this Door...", required: true, multiple: false, description: null)
        input(name: "screen", type: "capability.contactSensor", title: "Monitor this Screen...", required: true, multiple: false, description: null)
    }
    section("For how many minutes") {
        input "minutesOpen", "number", required: true, title: "Minutes?"
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(door, "contact.open", doorOpenHandler)
    subscribe(door, "contact.closed", doorCloseHandler)
    subscribe(screen, "contact.open", screenOpenHandler)
    subscribe(screen, "contact.closed", screenCloseHandler)
}

def doorOpenHandler(evt) {
    log.debug "Door Open Handler Called"
    if (screen.currentValue("contact") == "open") {
        // Both Door and Screen are open. Schedule warning
        log.debug "Scheduling to warn in ${minutesOpen} minutes"
        runIn(minutesOpen * 60, notifyOpenDoor)
    }
}

def doorCloseHandler(evt) {
    log.debug "Door Close Handler Called"
    if (screen.currentValue("contact") == "open") {
        // Door has been closed, remove any scheduled warnings.
        log.debug "Removing scheduled warning"
        unschedule(notifyOpenDoor)
    }
}

def screenOpenHandler(evt) {
    log.debug "Screen Open Handler Called"
    if (door.currentValue("contact") == "open") {
        // Both Door and Screen are open. Schedule warning
        log.debug "Scheduling to warn in ${minutesOpen} minutes"
        runIn(minutesOpen * 60, notifyOpenDoor)
    }
}

def screenCloseHandler(evt) {
    log.debug "Screen Close Handler Called"
    if (door.currentValue("contact") == "open") {
        // Screen has been closed, remove any scheduled warning.
        log.debug "Removing scheduled warning"
        unschedule(notifyOpenDoor)
    }    
}

def notifyOpenDoor() {
   log.debug "Notify Open Door called"
   if (screen.currentValue("contact") == "open" && door.currentValue("contact") == "open") {
       // Verified that both Door and Screen are still open, so issue the warning
       log.debug "Both doors are open, so sending warning"
       sendPush("${door} has been open with the ${screen} open for over ${minutesOpen} minutes")
   }
}