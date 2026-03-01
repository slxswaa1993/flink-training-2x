# Flink Training 2.x - Exercises

This is a custom training project for Apache Flink 2.x, providing hands-on exercises
to learn core Flink concepts. It follows the same structure as the official
[apache/flink-training](https://github.com/apache/flink-training) repository but
updated for Flink 2.x APIs.

## Prerequisites

- JDK 17 or higher
- Gradle 8.x (wrapper included)
- Apache Flink 2.0+ (optional, for cluster deployment)

## Building

```bash
./gradlew build
```

## Exercises

### Exercise 1: Ride Cleansing (Filtering)
**Location:** `ride-cleansing/`

**Concepts:**
- DataStream filtering with `filter()`
- Geographic data validation
- Basic transformations

**Run Solution:**
```bash
java -cp ride-cleansing/build/libs/ride-cleansing-2.0-SNAPSHOT-all.jar \
    org.apache.flink.training.exercises.ridecleansing.RideCleansingSolution
```

### Exercise 2: Rides and Fares (Stateful Enrichment)
**Location:** `rides-and-fares/`

**Concepts:**
- Connected streams with `connect()`
- Keyed state with `ValueState`
- State buffering for out-of-order events
- `RichCoFlatMapFunction`

**Run Solution:**
```bash
java -cp rides-and-fares/build/libs/rides-and-fares-2.0-SNAPSHOT-all.jar \
    org.apache.flink.training.exercises.ridesandfares.RidesAndFaresSolution
```

### Exercise 3: Hourly Tips (Windowed Analytics)
**Location:** `hourly-tips/`

**Concepts:**
- Event time processing
- Watermarks
- Tumbling windows
- `AggregateFunction`
- `ProcessWindowFunction`

### Exercise 4: Long Ride Alerts (ProcessFunction & Timers)
**Location:** `long-ride-alerts/`

**Concepts:**
- `KeyedProcessFunction`
- Event-time timers
- State management
- Timer callbacks with `onTimer()`

**Run Solution:**
```bash
java -cp long-ride-alerts/build/libs/long-ride-alerts-2.0-SNAPSHOT-all.jar \
    org.apache.flink.training.exercises.longridealerts.LongRideAlertsSolution
```

## Project Structure

```
flink-training-2x/
├── common/                    # Shared data types and generators
│   └── src/main/java/
│       └── org/apache/flink/training/
│           ├── datatypes/     # TaxiRide, TaxiFare
│           ├── sources/       # Data generators
│           └── utils/         # GeoUtils
├── ride-cleansing/           # Exercise 1
├── rides-and-fares/          # Exercise 2
├── hourly-tips/              # Exercise 3
└── long-ride-alerts/         # Exercise 4
```

## Key API Changes from Flink 1.x

1. **Source API**: Uses `DataGeneratorSource` instead of deprecated `SourceFunction`
2. **open() method**: Uses `OpenContext` instead of `Configuration`
3. **fromData()**: Replaces `fromElements()` for in-memory data
4. **FileSource**: New unified file reading API

## Learning Path

1. Start with **Ride Cleansing** - learn basic filtering
2. Move to **Rides and Fares** - understand stateful processing
3. Try **Hourly Tips** - master windowed analytics
4. Complete **Long Ride Alerts** - learn ProcessFunction and timers
