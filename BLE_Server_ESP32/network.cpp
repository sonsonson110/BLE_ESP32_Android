#include <HTTPClient.h>
#include <Arduino_JSON.h>
#include "network.h"

HTTPClient http;
const char* ssid = "";
const char* password = "";
const char* weatherEndpoint = "http://api.openweathermap.org/data/2.5/weather?units=metric&";
const char* weatherApiKey = "";

void buildWeatherEndpoint(float lon, float lat, char* endpoint, size_t endpointSize) {
  snprintf(endpoint, endpointSize, "%slat=%.5f&lon=%.5f&appid=%s", weatherEndpoint, lat, lon, weatherApiKey);
}

String httpGETRequest(const char* serverName) {
  HTTPClient http;

  // Your IP address with path or Domain name with URL path
  http.begin(serverName);

  // Send HTTP POST request
  int httpResponseCode = http.GET();

  String payload = "{}";

  if (httpResponseCode > 0) {
    Serial.print("HTTP Response code: ");
    Serial.println(httpResponseCode);
    payload = http.getString();
  } else {
    Serial.print("Error code: ");
    Serial.println(httpResponseCode);
  }
  // Free resources
  http.end();

  return payload;
}

void fetchWeather() {
  // Replace programatically later
  float lon = 105.8342;
  float lat = 21.0278;
  char endpoint[200];
  buildWeatherEndpoint(lon, lat, endpoint, sizeof(endpoint));

  String jsonBuffer = httpGETRequest(endpoint);
  JSONVar weatherObj = JSON.parse(jsonBuffer);

  if (JSON.typeof(weatherObj) == "undefined") {
    Serial.println("Parsing input failed!");
    return;
  }
  Serial.print("Temp: "); Serial.println(weatherObj["main"]["temp"]);
}