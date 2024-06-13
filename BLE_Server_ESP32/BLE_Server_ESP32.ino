#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <Wire.h>

#include "ssd1315.h"
#include "mpu6050.h"
#include "ble.h"

BLECharacteristic* pCharacteristic;
BLEServer *pServer;
// Display setup
#define SCREEN_WIDTH 128 // OLED display width, in pixels
#define SCREEN_HEIGHT 64 // OLED display height, in pixels
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, -1);
Adafruit_MPU6050 mpu;
sensors_event_t a, g, temp;

bool deviceConnected = false;
bool oldDeviceConnected = false;

void setup() {
  Serial.begin(115200);
  Serial.println("Initializing...");

  // Display setup
  initSsd1315(&display);
  initMpu6050(&mpu);
  MyBLEServerCallbacks* callbacks = new MyBLEServerCallbacks(&deviceConnected);
  initBle(pServer, pCharacteristic, callbacks);
  Serial.println("Initialized!");
}

void loop() {
  // Get new sensor events with the readings
  mpu.getEvent(&a, &g, &temp);

  if (deviceConnected) {
    // Notify changed value
    pCharacteristic->setValue(temp.temperature);
    pCharacteristic->notify();
  }

  // disconnecting
  if (!deviceConnected && oldDeviceConnected) {
      delay(500); // give the bluetooth stack the chance to get things ready
      pServer->startAdvertising(); // restart advertising
      Serial.println("Start advertising again...");
  }

  // connecting
  if (deviceConnected && !oldDeviceConnected) {
      // do stuff here on connecting
      oldDeviceConnected = deviceConnected;
  }
  display.clearDisplay(); // Clear the display buffer
  updateBleStatus(&display, deviceConnected, temp.temperature);
  updateAnimation(&display);
  display.display(); // Show the display buffer on the screen
  delay(200);        // Pause for 1/10 second
}