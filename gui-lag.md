# GUI Lag During Job Execution — Analysis & Recommendations

**Status:** Investigation only. No changes applied (deferred).

## Observed problem

When a LEVI job (overview, delta, eszett check, not-published, etc.) is running, the
GUI starts to lag / slow down / feels frozen until the job finishes.

## Key insight

The heavy work is **not** done on the JavaFX Application Thread. Every job is a
`javafx.concurrent.Task` started on its own daemon `Thread`:

- `MainController.startJob()` -> `runNextJob()` -> `new Thread(task).start()`
  (`levi-gui/.../MainController.java:713`)
- each `Task` calls into `CompareManager` on that background thread
  (`levi-gui/.../JobService.java`)

So CPU crunching and DB queries happen off the UI thread. The UI freezes because the
**FX Application Thread is flooded with update events** and increasingly expensive
text rendering.

## Root causes

### 1. GUI log appender pumps every core log line onto the FX thread (primary)

`GuiLogAppender` (`levi-gui/.../GuiLogAppender.java:59`) calls
`Platform.runLater(() -> logArea.appendText(line))` for:

- every `INFO` from `ch.ehealth.levi.core.*`, and
- every `WARN`+ from any logger.

The core logs frequently during a job:

- once per DB batch in `DbConnection` (`searchDescriptions` uses 10,000-row batches,
  `...batch {}/{}...`, `DbConnection.java:414`), and
- per-stage lines across `Comparator` / `CompareModelManager`.

Problems:

- `Platform.runLater` has an **unbounded queue**. When the worker thread logs faster
  than the FX loop drains, the queue balloons.
- `TextArea#appendText` is comparatively expensive (relayout + redraw) and gets
  **progressively slower as the control grows**.
- `logArea` grows unbounded for the whole session -> memory grows, layout gets slower.

Net effect: the FX thread spends most of its time churning through `appendText`,
so the whole window stutters/freezes during heavy jobs.

### 2. Statistics text area appends every progress message

`appendProgress` (`MainController.java:961`) does
`Platform.runLater(() -> statisticsArea.appendText(...))` for each `updateMessage`.
Same cost, smaller volume.

### 3. Memory / GC pressure

- Unbounded `TextArea` buffers (log + stats).
- The job holds a large in-memory `ResultCollector`.
- GC pauses can further stall the FX thread on top of the runLater flood.

### 4. (Minor) synchronous logging on the worker thread

`CONSOLE` + `FILE` appenders (`logback.xml`) are synchronous on the worker thread,
adding per-line formatting/IO overhead. Not the main cause.

## Recommended fixes (when we tackle this)

| # | Change | Impact | Effort |
|---|--------|--------|--------|
| A | **Bound & coalesce** `GuiLogAppender`: keep only the last ~N lines (e.g. 500) in `logArea` and flush to FX at a throttled rate (a few/sec or once per pulse) instead of one `runLater` per line | High | Small |
| B | Bound `statisticsArea` line count too (trim old lines in `appendProgress`) | Medium | Tiny |
| C | Wrap `CONSOLE`/`FILE`/`GUI` in a logback `AsyncAppender` so worker logging never blocks | Low-Medium | Small |

Expected result: the UI stays responsive during jobs, memory stays bounded, and the
log tail remains readable without unbounded growth.

## Notes
- The FX flood (A) is the dominant cause and should be addressed first.
- Optionally switch `logArea` from `TextArea` to a lighter virtualized view for large
  logs in the future.