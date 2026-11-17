using System.Collections.Concurrent;
using ForestFire.Api.Models;

namespace ForestFire.Api.Services;

// den seneste position for hver brandmand, til dashboard-kortet.
// Holdes kun i hukommelsen: en gammel position er ikke interessant, og den næste kommer om 10 sekunder.
public class FirefighterLocationService
{
    private readonly ConcurrentDictionary<string, FirefighterLocation> locations = new();

    public void Update(FirefighterLocation location)
    {
        locations[location.Id] = location;
    }

    public List<FirefighterLocation> GetAll()
    {
        return locations.Values.OrderBy(l => l.Id).ToList();
    }
}
