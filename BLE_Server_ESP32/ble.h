#include <Arduino.h>

class MyBLEServerCallbacks : public BLEServerCallbacks {
  private:
      bool* deviceConnected;

  public:
      // Constructor
      MyBLEServerCallbacks(bool* var);

      // Overridden methods from BLEServerCallbacks
      void onConnect(BLEServer* pServer) override;
      void onDisconnect(BLEServer* pServer) override;
};

void initBle(BLEServer *&pServer, BLECharacteristic*& pCharacteristic, MyBLEServerCallbacks* callbacks);