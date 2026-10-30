import type { SensorMeasurements, SensorReading } from "../interface/sensor";

export async function produce(
  measurements: SensorMeasurements,
): Promise<SensorReading> {
  return {
    ts: Math.floor(Date.now() / 1000),
    ...measurements,
  };
}