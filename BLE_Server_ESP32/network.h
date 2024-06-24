#include<Arduino.h>

extern const char* ssid;
extern const char* password;

void fetchWeather(char* description, size_t descriptionSize);