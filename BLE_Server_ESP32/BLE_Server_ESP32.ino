#include <BLEServer.h>
#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_SSD1306.h>
#include <Wire.h>
#include <WiFi.h>

#include "ssd1315.h"
#include "mpu6050.h"
#include "ble.h"
#include "network.h"

#define LED_PIN 2
#define TOUCH_PIN 4
#define TOUCH_THRESHOLD 15

// BLE setup
BLECharacteristic* pCharacteristic;
BLEServer* pServer;
bool deviceConnected = false;
bool oldDeviceConnected = false;
bool isAdvertising = true;
// Display setup
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, -1);
// MPU6050 setup
Adafruit_MPU6050 mpu;

void setup() {
  Serial.begin(115200);
  // Internal LED define
  pinMode(LED_PIN, OUTPUT);
  // Display setup
  initSsd1315(&display);
  // MPU6050
  initMpu6050(&mpu);
  // BLE
  MyBLEServerCallbacks* callbacks = new MyBLEServerCallbacks(&deviceConnected);
  initBle(pServer, pCharacteristic, callbacks);
  // Wifi
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) 
    delay(1000);
  // Fetch weather
  fetchWeather();

  Serial.println(F("Set up completed"));
}

void loop() {
  // Get new sensor events with the readings
  sensors_event_t a, g, temp;
  mpu.getEvent(&a, &g, &temp);

  // Read value from ESP32 touch sensor
  int touchValue = touchRead(TOUCH_PIN);
  if (touchValue <= TOUCH_THRESHOLD)
    digitalWrite(LED_PIN, HIGH);
  else
    digitalWrite(LED_PIN, LOW);

  // Drawing and do ble stuff if connected
  display.clearDisplay();  // Clear the display buffer
  if (deviceConnected) {
    // Notify changed value
    pCharacteristic->setValue(temp.temperature);
    pCharacteristic->notify();
    drawBlePairedStatus(&display);
    isAdvertising = false;
  } else {
    // Draw idle screen
    updateTempStatus(&display, temp.temperature);
    updateAnimation(&display);
  }
  display.display();  // Show the display buffer on the screen

  // disconnecting
  if (!deviceConnected && oldDeviceConnected && !isAdvertising) {
    delay(500);                   // give the bluetooth stack the chance to get things ready
    pServer->startAdvertising();  // restart advertising
    isAdvertising = true;
  }

  // connecting
  if (deviceConnected && !oldDeviceConnected) {
    // do stuff here on connecting
    oldDeviceConnected = deviceConnected;
  }
  delay(200);  // Pause for 1/10 second
}