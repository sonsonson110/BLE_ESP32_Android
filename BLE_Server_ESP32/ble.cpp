#include <BLEServer.h>
#include <BLE2902.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include "ble.h"

#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

// Constructor
MyBLEServerCallbacks::MyBLEServerCallbacks(bool* var) : deviceConnected(var) {}

// Overridden methods from BLEServerCallbacks
void MyBLEServerCallbacks::onConnect(BLEServer* pServer) {
    *deviceConnected = true;
}

void MyBLEServerCallbacks::onDisconnect(BLEServer* pServer) {
    *deviceConnected = false;
}

void initBle(BLEServer *&pServer, BLECharacteristic*& pCharacteristic, MyBLEServerCallbacks* callbacks) {
  BLEDevice::init("Pson ESP32-BLE device");
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(callbacks);
  BLEService *pService = pServer->createService(SERVICE_UUID);

  pCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID,
                      BLECharacteristic::PROPERTY_NOTIFY
                    );

  // CCC descriptor
  BLE2902 *pBLE2902 = new BLE2902();
  pBLE2902->setNotifications(true);
  pCharacteristic->addDescriptor(pBLE2902);

  pService->start();

  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(false);
  pAdvertising->setMinPreferred(0x00);
  BLEDevice::startAdvertising();
  Serial.println("Characteristic defined! Now you can read it in your phone!");
}