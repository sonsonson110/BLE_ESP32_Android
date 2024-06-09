# ESP32 BLE Server
## Architecture
- Client - Server
- For this example, ESP32 is the server, serves MPU-6050 sensor data to an Android device - a client
    - Server will continuously advertise its existance so other devices (client)
    - Client scans for service, establishs the connection and listens for incoming data
> There is also broadcast, and mesh mode. This repo only covers the point-to-point connection.

## Hierachy
![Hierachy](https://i0.wp.com/randomnerdtutorials.com/wp-content/uploads/2021/11/GATT-ESP32-BLE-Server-Client-Example.png?w=750&quality=100&strip=all&ssl=1)

image from https://randomnerdtutorials.com/esp32-ble-server-client/ tutorial
### GATT: generic attributes
- Defines an hierarchical data structure
- Is exposed to connect BLE devices

### UUID
- Service, Characteristic and Descriptor (if included) need an UUID
- Assign by [pre-defined rules](https://www.bluetooth.com/specifications/assigned-numbers/) or by choice

## NOTE
- "Multi-octet fields within the GATT Profile shall be sent least significant octet
first (little endian)."
    > For example, `F5-C1-EC-41` must be reversed to `41-EC-C1-F5`, which means `29.5947` in numeric

# Android BLE Client
Updating...