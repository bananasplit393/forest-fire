# Sensors used in data collection


These sensors will be replicated as producers so one could go and implement this system.

## Sensors built into:

Arduino Nicla Sense ME

Bosch BHI260AP: 6 axis IMU (3-Axis Accelerometer + 3-Axis Gyroscope) + MCU
Bosch BMP390: Pressure sensor
Bosch BMM150: Magnetometer
Bosch BME688: Humidity, temperature, and gas sensor (VOC)

## External Sensors

Bosch BMP388: Pressure sensor
Sensirion SPS30: Particular Matter sensor (Smoke detector)
Sensirion SHT31: Humidity and temperature sensor
Sensirion SPG30: Gas sensor (VOC)
GPS: For Timesync of the sensor readings


## Our implementation 

We need these datapoints:
ts, temp, humidity, pm25, lat, lon

UTC: timestamp in sec
temp: double
humidity: double (percentage)
pm10: Concentration of particle matter smaller than 1.0µm
pm25: Concentration of particle matter smaller than 2.5µm 
lat: latidude, double
lon: longitude, double

