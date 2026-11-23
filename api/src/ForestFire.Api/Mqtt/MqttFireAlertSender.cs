using System.Text.Json;
using ForestFire.Api.Interfaces;
using ForestFire.Api.Models;

namespace ForestFire.Api.Mqtt
{
    // bygger FireAlert-beskeden og publicerer den på alerts/{nodeId}, QoS 1 og retained,
    // så en app der forbinder senere stadig får alarmen. Beskeden er SensorData + alarm-flag.
    public class MqttFireAlertSender : IFireAlertSender
    {
        private readonly IMqttPublisher mqtt;
        private readonly string Topic;
        private readonly string DemoTopic;
        private static readonly JsonSerializerOptions jsonOptions = new(JsonSerializerOptions.Web);

        public MqttFireAlertSender(IMqttPublisher mqtt, IConfiguration config)
        {
            this.mqtt = mqtt;
            Topic = config["Mqtt:FireAlertTopic"] ?? "alerts/{0}";
            DemoTopic = config["Mqtt:DemoTopic"] ?? "alerts/demo";
        }

        public async Task SendAsync(SensorReadingDto reading)
        {
            if (string.IsNullOrWhiteSpace(reading.Node))
                return;

            var topic = string.Format(Topic, reading.Node);

            var message = new
            {
                alertId = reading.Id,   // dubletter (QoS 1) kan kendes på id'et
                node = reading.Node,
                ts = reading.Ts,
                temp = reading.Temp,
                humidity = reading.Humidity,
                pm10 = reading.Pm10,
                pm25 = reading.Pm25,
                lat = reading.Lat,
                lon = reading.Lon,
                fireProbability = reading.FireProbability,
                alarm = true
            };

            var json = JsonSerializer.Serialize(message, jsonOptions);

            try
            {
                await mqtt.PublishAsync(topic, json, retain: true);
                // demo: alle alarmer samlet på ét topic
                await mqtt.PublishAsync(DemoTopic, json, retain: true);
            }
            catch
            {
                // målingen er allerede gemt i databasen
            }
        }

        public async Task ClearAsync(string node)
        {
            if (string.IsNullOrWhiteSpace(node))
                return;

            var topic = string.Format(Topic, node);
            try
            {
                await mqtt.PublishAsync(topic, "", retain: true);
            }
            catch
            {
            }
        }
    }
}
