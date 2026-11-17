using ForestFire.Mqtt.Models;

namespace ForestFire.Mqtt.Interfaces
{
    public interface IFireAlertSender
    {
        Task SendAsync(Reading reading);
    }
}
