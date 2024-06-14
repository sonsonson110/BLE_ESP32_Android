#include <Arduino.h>

#define NUMICONS     4
#define LOGO_HEIGHT   16
#define LOGO_WIDTH    16

#define XPOS   0
#define YPOS   1
#define DELTAY 2
#define YOFFSET 20

void initSsd1315(Adafruit_SSD1306 *display);
void updateTempStatus(Adafruit_SSD1306 *display, float temp);
void updateAnimation(Adafruit_SSD1306 *display);
void drawBlePairedStatus(Adafruit_SSD1306* display);