/**
 *  Power Usage Limiter
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
    name: "Power Usage Limiter",
    namespace: "anil-mukundan",
    author: "Anil Mukundan",
    description: "Limits the amount of time a power outlet can be used in a day",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Allstate/power_allowance.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Allstate/power_allowance@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Allstate/power_allowance@2x.png")

preferences {
    section("Power Monitor") {
        input(name: "meter", type: "capability.powerMeter", title: "Monitor This Power Outlet...", required: true, multiple: false, description: null)
        input(name: "outlet", type: "capability.outlet", title: "Turn Off This Power Outlet...", required: true, multiple: false, description: null)
    }
    section("On Which Days") {
        input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
    }
    section("Daily Allowance in minutes") {
        input "minutesAllowed", "number", required: true, title: "Minutes?"
    }
    section("Power usage at idle") {
        input "idlePowerUsage", "number", required: true, title: "Watts?"
    }
    section("Send Notifications?") {
        input("recipients", "contact", title: "Send notifications to") {
        input "phone", "phone", title: "Warn with text message (optional)",
            description: "Phone Number", required: false
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
	subscribe(meter, "power", meterHandler)
    subscribe(outlet, "switch.on", outletOnHandler)
    state.meterStatus = "UNKNOWN"
    reset()
}

def outletOnHandler(evt) {
    if (days.contains(getWeekDay())) {
        if (isDaysQuotaUsed()) {
            if (state.meterStatus == "ON") {
                log.debug "Attempting to turn on ${outlet} while ${meter} is on and the quota has been used up"
                def message = "The ${outlet} has been switched on after days quota has been used!"
                if (location.contactBookEnabled && recipients) {
                    sendNotificationToContacts(message, recipients)
                } else {
                    if (phone) {
                        log.debug "Sending message to ${phone}"
                        sendSms(phone, message)
                    } 
                }
                log.debug "Switching ${outlet} off"
                outlet.off()
            }
       }
   }
}

def meterHandler(evt) {
    def day = getWeekDay()
    if (days.contains(day)) {
        def meterValue = evt.value as double

        // Save the metered device ON/OFF state
        if (meterValue > idlePowerUsage) {
            state.meterStatus = "ON"
        } else {
            state.meterStatus = "OFF"
        }


        if (state.meterStatus == "ON"  && !state.usageTimerSet) {
             // If the metered device is turned ON while there is not usage timer set
             // (a) if the days Quota is used, turn the outlet off
             // (b) if the Quota is not yet done, set a usage timer to off it when it it will be

             log.debug "${meter} showing power consumed at ${meterValue} watts which is more than ${idlePowerUsage}"

             // If we have the wrong day set in state, reset it
             if (day != state.today) {
                 reset()
             }

            if (isDaysQuotaUsed()) {
                // Done for the day
                log.debug "Today's quota has been used up. Switching off ${outlet}"
                outlet.off()
            } else if (!isDaysQuotaUsed()) {
                // Save current time so that we can compute how long the power is going to be used.
                state.usageStartTime = new Date().getDateTimeString();

                // Compute how much more time the outlet can be used.
                def remainingTime = minutesAllowed - state.minutesUsed;
                // Schedule to switch off outset after the remaining allowed time.
                log.debug "Scheduling to switch ${outlet} off in ${remainingTime} minutes"
                state.usageTimerSet = true
                runIn(remainingTime * 60, switchOff)
           }

        } else if (!isDaysQuotaUsed() && state.meterStatus == "OFF" && state.usageTimerSet) {
            // If the metered device is turned Off while the useage Timer is set, save the time it was used and
            // remove the usage timer.
            log.debug "${meter} showing power consumed at ${meterValue} watts which is less than ${idlePowerUsage} watts"

           // Compute much time was spend in last usage and update the minutes used so far today.
           if (state.usageStartTime != null && state.usageStartTime.length() > 0) {
               state.minutesUsed = state.minutesUsed + timeDiffMinutes(Date.parse("M/d/yy h:mm:ss a", state.usageStartTime), new Date()).round(0)
               log.debug "${meter} was used for ${state.minutesUsed} minutes so far today"
           } 

           // Remove any scheduled switch Offs
           log.debug "Removing any scheduled switch offs"
           state.usageTimerSet = false;
           unschedule(switchOff)

        } else if (isDaysQuotaUsed() && state.meterStatus == "OFF") {
            // If the meter is turned Off, after the days Quota has been reached (in which case the outlet would have been turned off)
            // then we can turn the outlet On again. 
            log.debug "Turning on ${outlet} since ${meter} has been turned off"
            outlet.on()
        }
    }
}

def switchOff() {
     // Done for the day
     state.minutesUsed = minutesAllowed;
     state.usageTimerSet = false;
     
    // Switch off the outset
    log.debug "Switching off ${outlet}"
    outlet.off()
    
    // Schdule the outlet to be turned on again tomorrow
    log.debug "Scheduling to switch ${outlet} on again tomorrow."
    runOnce(timeTodayAfter("23:59", "00:00", location.timeZone), switchOn)
}

def switchOn() {
    // Switch on the outlet
    log.debug "Switching on ${meter}"
    outlet.on()
    
    // Reset the state for a new day!
	reset()
}

/*
 * Finds the difference in minutes between two Dates. 
 * We just need the delta so don't care about the timezone
 */
def timeDiffMinutes(Date start, Date stop) {
	def result = ((stop.time - start.time) / 1000.0 / 60.0) as double;
	return result;
}

/*
 * Gets the Weekday for the hub's timezone
 */
def getWeekDay() {
    def df = new java.text.SimpleDateFormat("EEEE")
    df.setTimeZone(location.timeZone)
    return df.format(new Date())
}

/*
 * Reset everything
 */
def reset() {
    state.usageTimerSet = false;
    state.minutesUsed = 0;
    state.today = getWeekDay()
}

/*
 * Is the day's quota used?
 */
 def isDaysQuotaUsed() {
     return (state.minutesUsed >= minutesAllowed)
 }
 