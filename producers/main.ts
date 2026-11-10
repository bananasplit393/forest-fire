import { produce, summerEnv } from "./main/produce";

console.log("summerEnv():", summerEnv());
produce().then((reading) => console.log("produce():", reading));