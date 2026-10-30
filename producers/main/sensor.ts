import type {
  SensorMeasurements,
  SensorProducer,
  SensorReading,
} from "../interface/sensor";
import { produce as produceReading } from "./produce";

export class Sensor implements SensorProducer {
  constructor(
    readonly name: string,
    readonly measurements: SensorMeasurements = {},
  ) {}

  produce(): Promise<SensorReading> {
    return produceReading(this.measurements);
  }
}