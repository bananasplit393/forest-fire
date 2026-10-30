import type { SensorMeasurements, SensorReading } from "../interface/sensor";
import { randomLocation } from "./locations";

const random = (min: number, max: number) => min + Math.random() * (max - min);
const round = (value: number, decimals = 1) =>
  Math.round(value * 10 ** decimals) / 10 ** decimals;

/**
 * Generates measurements typical for a Danish summer (Jun-Aug): mild
 * temperatures with a daily cycle peaking mid-afternoon, fairly high humidity
 * that is lowest when it is warmest, clean air and a random predefined location.
 */
export function summerEnv(date: Date = new Date()): SensorMeasurements {
  const hour = Number(
    new Intl.DateTimeFormat("en-GB", {
      hour: "numeric",
      hourCycle: "h23",
      timeZone: "Europe/Copenhagen",
    }).format(date),
  );

  // -1 at 03:00 (coolest) to +1 at 15:00 (warmest)
  const daily = -Math.cos(((hour - 3) / 24) * 2 * Math.PI);

  const temp = 17 + 5 * daily + random(-2, 2);
  const humidity = 70 - 15 * daily + random(-5, 5);
  const pm25 = random(3, 15);
  const pm10 = pm25 + random(2, 10);

  const { lat, lon } = randomLocation();

  return {
    temp: round(temp),
    humidity: round(Math.min(100, Math.max(30, humidity))),
    pm10: round(pm10),
    pm25: round(pm25),
    lat,
    lon,
  };
}

export async function produce(
  measurements: SensorMeasurements = summerEnv(),
): Promise<SensorReading> {
  return {
    ts: Math.floor(Date.now() / 1000),
    ...measurements,
  };
}