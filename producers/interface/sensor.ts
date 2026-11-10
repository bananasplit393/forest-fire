export interface SensorMeasurements {
  readonly temp?: number;
  readonly humidity?: number;
  readonly pm10?: number; // particle Matter < 1.0
  readonly pm25?: number; // particle matter < 2.5micrometers
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
