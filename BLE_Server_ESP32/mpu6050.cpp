#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <mpu6050.h>

void initMpu6050(Adafruit_MPU6050 *mpu) {
  if (!mpu->begin()) {
    while (1) {
      delay(10);
    }
  }

  // Set up sensor range
  mpu->setAccelerometerRange(MPU6050_RANGE_8_G);
  mpu->setGyroRange(MPU6050_RANGE_500_DEG);
  mpu->setFilterBandwidth(MPU6050_BAND_21_HZ);
}
