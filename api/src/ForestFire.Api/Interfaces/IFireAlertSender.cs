using ForestFire.Api.Models;

namespace ForestFire.Api.Interfaces
{
    public interface IFireAlertSender
    {
        Task SendAsync(SensorReadingDto reading);
        Task ClearAsync(string node);
    }
}
