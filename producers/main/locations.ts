import type { NamedLocation } from "../interface/location";

export const locations: readonly NamedLocation[] = [
  { name: "Rold Skov", lat: 56.8, lon: 9.85 },
  { name: "Gribskov", lat: 55.98, lon: 12.32 },
  { name: "Klosterheden Plantage", lat: 56.38, lon: 8.4 },
  { name: "Risskov", lat: 56.19, lon: 10.24 },
  { name: "Hasle Skov", lat: 56.18, lon: 10.15 },
];

export function randomLocation(
  options: readonly NamedLocation[] = locations,
): NamedLocation {
  const location = options[Math.floor(Math.random() * options.length)];
  if (!location) throw new Error("No locations available");
  return location;
}
