using ForestFire.Api.Models;
using Microsoft.ML.OnnxRuntime;
using Microsoft.ML.OnnxRuntime.Tensors;

namespace ForestFire.Api.Services;

// Input [temp, humidity, pm25]. Mangler en værdi, sendes NaN, og modellen bruger selv medianen
public class RiskModelService
{
    private readonly InferenceSession session;

    public RiskModelService(IConfiguration config)
    {
        session = new InferenceSession(config["Model:Path"] ?? "Model/model.onnx");
    }

    public double Score(SensorReadingModel reading)
    {
        var input = new DenseTensor<float>(
            new[] { (float)(reading.Temp ?? double.NaN), (float)(reading.Humidity ?? double.NaN), (float)(reading.Pm25 ?? double.NaN) },
            new[] { 1, 3 }); // betyder 1 række og 3 kolonner (temp, humidity, pm25)

        var inputName = session.InputMetadata.Keys.First();
        using var results = session.Run(new[] { NamedOnnxValue.CreateFromTensor(inputName, input) });
        return results.First().AsTensor<float>().GetValue(0);
    }
}
