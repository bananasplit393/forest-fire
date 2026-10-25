# Ideas to possibly implement

Two small additions that fit the course demo. Neither is built yet. Both are roughly an hour of work.

## 1. Tell the backend the firefighter is responding

**Problem.** The backend only knows a firefighter is responding because location messages start
arriving, and only knows they stopped because the messages stop. A phone that loses signal looks the
same as a firefighter who ended the response.

**Idea.** Add a `status` field to the location message the app already publishes on
`forest/<zone>/firefighters/<id>`:

```json
{"id":"ff-01","lat":56.17,"lon":10.19,"ts":1792750000,"status":"responding"}
```

- `responding` on every location message while the alarm is active.
- One final message with `"status":"ended"` when the firefighter taps **End response** or the alarm
  is cleared.

Keeping it in the existing topic means the backend's current subscription `forest/+/firefighters/+`
already receives it. A separate sub-topic such as `.../firefighters/<id>/status` would not match that
`+` and would need a backend change.

**Why it's worth it.** The IoT loop becomes visibly two-way in the demo: the backend (or
`smoke-test.sh`) can print "ff-01 responding" / "ff-01 ended", which completes the sequence diagram.

**Where it goes.**
- `data/AlertJson.kt`: add `status` to the location JSON.
- `service/DutyService.kt`: pass `responding` in `publishLocation`, publish one `ended` message in the
  End response / clear path before location updates stop.
- Unit test for the JSON shape.
- Agree the field with the backend team (they can ignore it until they use it).

**Open question.** Should `ended` distinguish "firefighter ended it" from "backend cleared it"?
Probably not needed for the demo.

## 2. Warn when the alarm data may be outdated

**Problem.** If the broker connection drops during an alarm, the app keeps showing the last fire
position with no hint that it's no longer live. A firefighter could walk towards a position the
backend has since moved or cleared.

**Idea.** Show a line on the alarm card and the compass header, "Data may be outdated · updated 6 min
ago", when either:

- the connection is not *Connected* while an alarm is active, or
- the alert's `ts` is older than a threshold (for example 5 minutes).

**Why it's worth it.** It's a safety feature you can demo in seconds: turn on airplane mode during an
alarm and the warning appears; turn it off and it disappears after reconnect.

**Where it goes.**
- A pure function, e.g. `isStale(alertTs, now, connection): Boolean`, with unit tests.
- `MainUiState` / `CompassUiState`: a `stale` flag.
- `AlarmCard` and the compass header: the warning text (amber, matching the *Reconnecting* stripe).
- A screenshot test fixture for the stale state.

**Open question.** Does the backend republish the alert regularly while the fire is active? If it only
publishes on change, an old `ts` is normal and only the connection check should count. Check with
the backend team before choosing the threshold.
