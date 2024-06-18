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

int uiState = 0;

void setup() {
  Serial.begin(115200);
  // Internal LED define
  pinMode(LED_PIN, OUTPUT);
  // Display setup
  initSsd1315(&display);

  // MPU setup
  displayInitProcess(&display, "mpu6050", 0.4);
  initMpu6050(&mpu);
  delay(1000);
  // BLE
  displayInitProcess(&display, "ble", 0.6);
  MyBLEServerCallbacks* callbacks = new MyBLEServerCallbacks(&deviceConnected);
  initBle(pServer, pCharacteristic, callbacks);
  delay(1000);
  // Wifi
  displayInitProcess(&display, "wifi", 0.8);
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED)
    delay(1000);
  // Fetch weather
  displayInitProcess(&display, "web api", 0.9);
  fetchWeather();
  fetchLocation();

  displayInitProcess(&display, "loop()", 1);
  delay(2000);
}

void loop() {
  // Get new sensor events with the readings
  sensors_event_t a, g, temp;
  mpu.getEvent(&a, &g, &temp);

  // Read value from ESP32 touch sensor
  int touchValue = touchRead(TOUCH_PIN);
  if (touchValue <= TOUCH_THRESHOLD) {
    uiState = uiState + 1;
    digitalWrite(LED_PIN, HIGH);
  } else
    digitalWrite(LED_PIN, LOW);

  display.clearDisplay();
  switch (uiState % 2) {
    case 0:
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

      // disconnecting
      if (!deviceConnected && oldDeviceConnected && !isAdvertising) {
        delay(500);
        pServer->startAdvertising();
        isAdvertising = true;
      }

      // connecting
      if (deviceConnected && !oldDeviceConnected) {
        // do stuff here on connecting
        oldDeviceConnected = deviceConnected;
      }
      break;
    case 1:
      mpu6050Screen(&display, a, g);
      break;
  }
  display.display();
  delay(100);  // Pause for 1/10 second
}