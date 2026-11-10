export interface Location {
  readonly lat?: number;
  readonly lon?: number;
}

export interface NamedLocation extends Location {
  readonly name: string;
}