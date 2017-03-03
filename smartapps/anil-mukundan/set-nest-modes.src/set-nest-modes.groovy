/**
 *  Set Nest Modes
 *
 *  Copyright 2015 Anil Mukundan
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
    name: "Set Nest Modes",
    namespace: "anil-mukundan",
    author: "Anil Mukundan",
    description: "Sets Nest Modes to match Smart Things Home and Away modes",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
  section("Select the thermostants to update") {
    input "thermostats", "capability.thermostat", multiple: true
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
    // Save the current state for comparion later
    state.lastMode = location.mode
	// Subscribe to mode changes
    subscribe(location, "mode", modeChangeHandler)
}

// TODO: implement event handlers
def modeChangeHandler(evt) {
	// Switch themorstat to Away mode, if the new mode is "Away"
    if (evt.value == "Away")  {
        // Always put Nest to away mode
		sendNotificationEvent("Setting Nest to Away")
		thermostats?.away()
	} else if (evt.value == "Home") {
        // Put nest in Home mode only if the previous Smart Things
        // mode was "Away"
        if (state.lastMode == 'Away') {
		    sendNotificationEvent("Setting Nest to Home")
		    thermostats?.present()
        }
	} else if (evt.value == "Kids at Home") {
        // Put nest in Home mode only if the previous Smart Things
        // mode was "Away"
        if (state.lastMode == 'Away') {
		    sendNotificationEvent("Setting Nest to Home")
		    thermostats?.present()
        }
	}
    // Save the current state for comparion later
    state.lastMode = location.mode    
}
