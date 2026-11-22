export interface SensorMeasurements {
  readonly temp?: number;
  readonly humidity?: number;
  readonly pm10?: number; // particulate matter < 10 micrometers
  readonly pm25?: number; // particulate matter < 2.5 micrometers
  readonly lat?: number;
  readonly lon?: number;
  readonly node?: string;
}

export interface SensorReading extends SensorMeasurements {
  /** Unix timestamp in seconds (UTC). */
  readonly ts: number;
}

export interface SensorProducer {
  readonly name: string;

  produce(): Promise<SensorReading>;
}