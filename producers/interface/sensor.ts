export interface SensorMeasurements {
  readonly temp?: number;
  readonly humidity?: number;
  readonly pm10?: number;
  readonly pm25?: number;
  readonly lat?: number;
  readonly lon?: number;
}

export interface SensorReading extends SensorMeasurements {
  /** Unix timestamp in seconds (UTC). */
  readonly ts: number;
}

export interface SensorProducer {
  readonly name: string;

  produce(): Promise<SensorReading>;
}
