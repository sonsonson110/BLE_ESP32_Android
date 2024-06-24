#include <HTTPClient.h>
#include <Arduino_JSON.h>
#include "network.h"

HTTPClient http;
const char* ssid = "Pson";
const char* password = "bosuacamon";
const char* weatherEndpoint = "http://api.openweathermap.org/data/2.5/weather?units=metric&";
const char* weatherApiKey = "9ea8f4657c218a1a8eb048d9ebaba4d9";
const char* locationEndpoint = "http://ip-api.com/json/";

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

void fetchLocation(float* lon, float* lat) {
  String jsonBuffer = httpGETRequest(locationEndpoint);
  JSONVar locationObj = JSON.parse(jsonBuffer);

  if (JSON.typeof(locationObj) == "undefined") {
    Serial.println("Parsing input failed!");
    return;
  }

  *lon = (double)locationObj["lon"];
  *lat = (double)locationObj["lat"];
}

void fetchWeather(char* description, size_t descriptionSize) {
  float lon;
  float lat;
  fetchLocation(&lon, &lat);

  char endpoint[200];
  buildWeatherEndpoint(lon, lat, endpoint, sizeof(endpoint));

  String jsonBuffer = httpGETRequest(endpoint);
  // JSONVar weatherObj = JSON.parse(jsonBuffer);

  // if (JSON.typeof(weatherObj) == "undefined") {
  //   Serial.println("Parsing input failed!");
  //   return;
  const char* result = jsonBuffer.c_str();
  snprintf(description, descriptionSize, "%s", result);
}
