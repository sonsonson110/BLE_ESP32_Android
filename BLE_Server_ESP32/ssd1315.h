#include <Arduino.h>

#define SCREEN_WIDTH 128  
#define SCREEN_HEIGHT 64

#define NUMICONS     4
#define LOGO_HEIGHT   16
#define LOGO_WIDTH    16

#define XPOS   0
#define YPOS   1
#define DELTAY 2
#define YOFFSET 20

#define GYRO 0
#define ACCELERATION 1
#define MAX_ACC_VAL 15 // m/s
#define MAX_GYRO_VAL 3

void displayInitProcess(Adafruit_SSD1306* display, char* componentName, float p);
void initSsd1315(Adafruit_SSD1306 *display);
void updateTempStatus(Adafruit_SSD1306 *display, float temp);
void updateAnimation(Adafruit_SSD1306 *display);
void drawBlePairedStatus(Adafruit_SSD1306* display);
void mpu6050Screen(Adafruit_SSD1306* display, sensors_event_t a, sensors_event_t g);
