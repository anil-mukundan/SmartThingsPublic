/**
 *  Open Garage Door on Arrival
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
    name: "Open Garage Door on Arrival",
    namespace: "anil-mukundan",
    author: "Anil Mukundan",
    description: "Open Garage Door When Someone Arrives",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png")


preferences {
    section("Open this Garage Door") {
        input(name: "garage", type: "capability.garageDoorControl", required: true, multiple: false, description: null)
    }
    section("When this person come back") {
        input (name: "people", type: "capability.presenceSensor", required: true, multiple: false, description: null)
    }
    section("Between these times") {
        input (name: "fromTime", type: "time", title: "From", required: true)
        input (name: "toTime", type: "time", title: "To", required: true)
    }
    section("On these days") {
        input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday" : "Saturday", "Sunday" : "Sunday"]
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
    subscribe(people, "presence", OnPresenceChange)
}

def OnPresenceChange(evt) {
    if (evt.value == "present") {
        log.debug "${people} has arrived"
        if (days.contains(getWeekDay()) && timeOfDayIsBetween(fromTime, toTime, new Date(), location.timeZone)) {
            log.debug "Between ${fromTime} and ${toTime} on selected days"
            if (garage.currentValue("door") == "closed") {
                log.debug "${garage} is currently closed. Opening it"
                garage.open()
            } else {
              log.debug "${garage} is already open"
            }
        }
    }
}

/*
 * Gets the Weekday for the hub's timezone
 */
def getWeekDay() {
    def df = new java.text.SimpleDateFormat("EEEE")
    df.setTimeZone(location.timeZone)
    return df.format(new Date())
}
