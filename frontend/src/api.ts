// API'ets adresse. Sæt VITE_API_URL for at ændre den
const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8081";

export interface Reading {
  id: string;
  node: string | null;
  timestamp: string;
  temp: number | null;
  humidity: number | null;
  pm10: number | null;
  pm25: number | null;
  lat: number | null;
  lon: number | null;
  fireProbability: number | null;
  isFire: boolean | null;
}

export interface Firefighter {
  id: string;
  lat: number;
  lon: number;
  receivedAt: string;
}

// FireAlert fra mqtt-servicen på alerts/{node}, retained
export interface FireAlert {
  alertId: string;
  node: string;
  ts: number;
  temp?: number;
  humidity?: number;
  pm10?: number;
  pm25?: number;
  lat?: number;
  lon?: number;
  fireProbability: number;
  alarm?: boolean;
  receivedAt?: number;
}

export const getReadings = (): Promise<Reading[]> =>
  fetch(`${API_URL}/readings?limit=100`).then((r) => r.json());

export const getFirefighters = (): Promise<Firefighter[]> =>
  fetch(`${API_URL}/firefighters`).then((r) => r.json());
