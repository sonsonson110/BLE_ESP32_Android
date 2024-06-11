#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <mpu6050.h>

void initMpu6050(Adafruit_MPU6050 *mpu) {
  if (!mpu->begin()) {
    Serial.println("Failed to find MPU6050 chip");
    while (1) {
      delay(10);
    }
  }
  Serial.println("MPU6050 Found!");

  // Set up sensor range
  mpu->setAccelerometerRange(MPU6050_RANGE_8_G);
  mpu->setGyroRange(MPU6050_RANGE_500_DEG);
  mpu->setFilterBandwidth(MPU6050_BAND_21_HZ);
}

void logMpu6050(sensors_event_t a, sensors_event_t g, sensors_event_t temp) {
  Serial.println("--start logMPU6050--");
  Serial.printf("ACCELEROMETER X: %.5f, Y: %.5f, Z: %.5f m/s^2\n", a.acceleration.x, a.acceleration.y, a.acceleration.z);
  Serial.printf("GYROSCOPE X: %.5f, Y: %.5f, Z: %.5f rad/s\n", g.gyro.x, g.gyro.y, g.gyro.z);
  Serial.printf("TEMPERATURE: %.4f degC\n", temp.temperature);
  Serial.println("--end logMPU6050--\n");
}
