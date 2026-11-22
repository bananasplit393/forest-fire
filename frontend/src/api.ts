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

// en node med brand, ud fra dens nyeste måling
export interface FireAlert {
  node: string;
  lat: number | null;
  lon: number | null;
  fireProbability: number;
}
export const getReadings = (): Promise<Reading[]> =>
  fetch(`${API_URL}/readings?limit=100`).then((r) => r.json());

export const getFirefighters = (): Promise<Firefighter[]> =>
  fetch(`${API_URL}/firefighters`).then((r) => r.json());
