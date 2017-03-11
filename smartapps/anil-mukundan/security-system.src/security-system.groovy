/**
 *  Security System
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
    name: "Security System",
    namespace: "anil-mukundan",
    author: "Anil Mukundan",
    description: "Security Smart App that monitors my home while I am away.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-AudioVisualSmokeAlarm.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-AudioVisualSmokeAlarm@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-AudioVisualSmokeAlarme@2x.png")


preferences {
    section("Sensors to Monitor") {
        input(name: "doors", type: "capability.contactSensor", title: "Monitor These Doors...", required: true, multiple: true, description: null)
        input(name: "motions", type: "capability.motionSensor", title: "Monitor these Motion Sensors...", required: true, multiple: true, description: null)
    }
    section("Alarms to Alert") {
        input(name: "alarms", type: "capability.alarm", title: "Trigger these Alarms...", required: true, multiple: true, description: null)
        input (name: "alarmDuration", type: "number", required: true, title: "For how many minutes?")
        input (name: "leadTime", type: "number", required: true, title: "Wait how many minutes before sounding alarm?")
    }
    section("Notify on Breach") {
        input("recipients", "contact", title: "Send notifications to") {
            input (name: "phone", type: "phone", title: "Phone number to Alert",
                description: "Phone Number", required: true)
            input (name: "altPhone", type: "phone", title: "Alernate Phone number to Alert",
                description: "Phone Number", required: false)    
        }
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
	subscribe(doors, "contact.open", doorHandler)
    subscribe(motions, "motion.active", motionHandler)
    state.alarmStatus = "OFF"
}

def doorHandler(evt) {
	if (location.currentMode == "Away") {
       handleSecurityBreach()
    }
}

def motionHandler(evt) {
    if (location.currentMode == "Away") {
       handleSecurityBreach()
    }
}

def handleSecurityBreach() {
    log.debug "Security Breach Detected!"
    if (state.alarmStatus == "OFF") { 
        notifySecurityBreach()
        log.debug "Trigger Security Alarm in ${leadTime} minutes"
	    runIn(leadTime * 60, onAlerts)
        state.alarmStatus = "ON"
    } else {
        log.debug "Security Breach Alarms already scheduled"
    }
}

def notifySecurityBreach() {
    log.debug "Notify Security Breach to Phone: ${phone}"
    def message = "Possible Security Breach at Home. Alarms will be triggered in a minute, Switch to Home mode to cancel"
    sendSms(phone, message)
    if (altPhone) {
       log.debug "Notify Security Breach to Phone: ${AltPhone}"
       sendSms(altPhone, message)      
    }
}

def onAlerts() {
    log.debug "Recieved message to trigger Security Alarms"
    if (location.currentMode == "Away") {
        log.debug "Triggering Security Alarms"
    	alarms.siren()
    	log.debug "Trigger Security Alarm to switch off in ${alarmDuration} minutes" 
    	runIn(alarmDuration * 60, offAlerts)
    } else {
        log.debug "Home is no longer in Away Mode. Cancelling Security Alarms"
    }
}

def offAlerts() {
    log.debug "Recieved message to trigger Security Alarms"
    log.debug "Switching off Security Alarms"
    alarms.off()
    state.alarmStatus = "OFF"
}