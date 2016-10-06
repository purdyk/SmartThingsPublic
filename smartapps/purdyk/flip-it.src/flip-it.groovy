/**
 *  Copyright 2015 Kevin Purdy
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
 *  I'm Chilly
 *
 *  Author: Kevin Purdy
 */
definition(
    name: "Flip It!",
    namespace: "purdyk",
    author: "Kevin Purdy",
    description: "Monitor the temperature and when it crosses a threshold flip a switch.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
	section("Monitor the temperature...") {
		input "temperatureSensor1", "capability.temperatureMeasurement"
	}
    section("When the temperature is...") {
    	input "direction", "enum",  options: ["Above","Below"]
    }
	section("this thireshold...") {
		input "temperature1", "number", title: "Temperature?"
	}
	section("Toggle switch...") {
		input "switch1", "capability.switch", required: false
	}
    section("to...") {
    	input "onoff", "enum", options: ["On", "Off"]
    }
}

def installed() {
	subscribe(temperatureSensor1, "temperature", temperatureHandler)
}

def updated() {
	unsubscribe()
	subscribe(temperatureSensor1, "temperature", temperatureHandler)
}

def temperatureHandler(evt) {
	log.trace "temperature: $evt.value, $evt"

	def tempValue = temperature1
	def mySwitch = settings.switch1
    def above = settings.direction == "Above"
	def on = settings.onoff == "On"
    
	// TODO: Replace event checks with internal state (the most reliable way to know if an SMS has been sent recently or not).
	if ((!above && evt.doubleValue <= tempValue) || (above && event.doubleValue >= tempValue)) {
		log.debug "Checking how long the temperature sensor has been reporting <= $tooCold"

		// Don't send a continuous stream of text messages
		def deltaMinutes = 10 // TODO: Ask for "retry interval" in prefs?
		def timeAgo = new Date(now() - (1000 * 60 * deltaMinutes).toLong())
		def recentEvents = temperatureSensor1.eventsSince(timeAgo)?.findAll { it.name == "temperature" }
		log.trace "Found ${recentEvents?.size() ?: 0} events in the last $deltaMinutes minutes"
		def debounce = recentEvents.count { it.doubleValue <= tooCold } > 1

		if (!debounce) {
			log.debug "Temperature dropped below $tooCold: turning off $mySwitch"
            if (on) {
				switch1?.on()
            } else {
            	switch1?.off()
            }
		}
	}
}