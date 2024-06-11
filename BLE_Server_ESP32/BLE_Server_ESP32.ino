#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include "mpu6050.h"
#include "ble.h"

BLECharacteristic* pCharacteristic;
BLEServer *pServer;

Adafruit_MPU6050 mpu;
sensors_event_t a, g, temp;

bool deviceConnected = false;
bool oldDeviceConnected = false;

void setup() {
  Serial.begin(115200);
  Serial.println("Initializing...");
  initMpu6050(&mpu);
  MyBLEServerCallbacks* callbacks = new MyBLEServerCallbacks(&deviceConnected);
  initBle(pServer, pCharacteristic, callbacks);

  Serial.println("Initialized!");
}

void loop() {
  delay(5000);
  if (deviceConnected) {
      // Get new sensor events with the readings
    mpu.getEvent(&a, &g, &temp);

    // notify changed value
    pCharacteristic->setValue(temp.temperature);
    pCharacteristic->notify();
    
    // logs
    logMpu6050(a, g, temp);
  }

  // disconnecting
  if (!deviceConnected && oldDeviceConnected) {
      delay(500); // give the bluetooth stack the chance to get things ready
      pServer->startAdvertising(); // restart advertising
      Serial.println("Start advertising again...");
      oldDeviceConnected = deviceConnected;
  }

  // connecting
  if (deviceConnected && !oldDeviceConnected) {
      // do stuff here on connecting
      oldDeviceConnected = deviceConnected;
  }
}