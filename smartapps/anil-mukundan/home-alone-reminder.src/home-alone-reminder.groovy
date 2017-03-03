/**
 *  Home Alone Reminder
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
    name: "Home Alone Reminder",
    namespace: "anil-mukundan",
    author: "Anil Mukundan",
    description: "Reminds to switch off Home Alone when someone returns to home. Automatically turns it off after 10 minutes",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("When one of these people come back") {
        input "people", "capability.presenceSensor", multiple: true
    }
}

def installed() {
    subscribe(people, "presence", presence)
    subscribe(location, "mode", modeChangeHandler)
}

def updated() {
    unsubscribe()
    subscribe(people, "presence", presence)
}

def presence(evt) {
    if (evt.value == "not present" && location.mode == "Kids at Home") {
        //If everyone has left, then set flag for reminders when anyone returns
        if (everyoneIsAway()) {
            state.homeAloneReminder = true;
        }
    }

    if (evt.value == "present" && location.mode == "Kids at Home" && state.homeAloneReminder) {
        // When some one returns send the reminder
        sendPush(" Home Alone is currently on. Turn it off if no longer needed");
        // If still not turned off in 10 minutes then automatically turn it off
        runIn(60*10, switchToHome,  [overwrite: false])
    }
}

def modeChangeHandler(evt) {
    if (evt.value == "Kids at Home")  {
        // Initialize the flag to false. Will set it to true when everyone leaves.
    	state.homeAloneReminder = false;
	}
}

def switchToHome() {
    if (!everyoneIsAway() && location.currentMode == "Kids at Home") {
        sendPush("Turning off Home Alone because adults are back home for a while now!");
    	location.helloHome?.execute("I'm Back!")
    }
}

private everyoneIsAway() {
    def result = true
    for (person in people) {
        if (person.currentPresence == "present") {
            result = false
            break
        }
    }
    return result
}