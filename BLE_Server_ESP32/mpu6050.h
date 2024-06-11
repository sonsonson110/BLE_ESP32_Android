#include <Arduino.h>

void initMpu6050(Adafruit_MPU6050 *mpu);
void logMpu6050(sensors_event_t a, sensors_event_t g, sensors_event_t temp);